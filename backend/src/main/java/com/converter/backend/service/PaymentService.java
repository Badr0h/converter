package com.converter.backend.service;

import com.converter.backend.dto.payment.PaymentCreateDto;
import com.converter.backend.dto.payment.PaymentResponseDto;
import com.converter.backend.exception.ResourceNotFoundException;
import com.converter.backend.model.Payment;
import com.converter.backend.model.Payment.Status;
import com.converter.backend.model.Plan;
import com.converter.backend.model.Subscription;
import com.converter.backend.model.User;
import com.converter.backend.repository.PaymentRepository;
import com.converter.backend.repository.PlanRepository;
import com.converter.backend.repository.SubscriptionRepository;
import com.converter.backend.repository.UserRepository;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    
    @Value("${paypal.client.id}")
    private String paypalClientId;
    
    @Value("${paypal.client.secret}")
    private String paypalClientSecret;
    
    @Value("${paypal.return.url:http://localhost:4200/payment-success}")
    private String returnUrl;
    
    @Value("${paypal.cancel.url:http://localhost:4200/payment-failed}")
    private String cancelUrl;
    
    @Value("${paypal.mode:sandbox}")
    private String paypalMode;

    private PayPalHttpClient payPalClient;

    /**
     * Lazy initialization of PayPal client
     */
    private PayPalHttpClient getPayPalClient() {
        if (payPalClient == null) {
            PayPalEnvironment environment = "sandbox".equalsIgnoreCase(paypalMode)
                    ? new PayPalEnvironment.Sandbox(paypalClientId, paypalClientSecret)
                    : new PayPalEnvironment.Live(paypalClientId, paypalClientSecret);
            payPalClient = new PayPalHttpClient(environment);
            log.info("PayPal client initialized in {} mode", paypalMode);
        }
        return payPalClient;
    }

    /**
     * Retrieve all payments
     */
    public List<PaymentResponseDto> getAllPayments() {
        log.debug("Fetching all payments");
        return paymentRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Retrieve all payments for a specific user
     */
    public List<PaymentResponseDto> getPaymentsByUserId(Long userId) {
        log.debug("Fetching payments for user: {}", userId);
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Retrieve payment by ID
     */
    public PaymentResponseDto getPaymentById(Long id) {
        log.debug("Fetching payment with id: {}", id);
        Payment payment = findPaymentById(id);
        return mapToResponseDto(payment);
    }

    /**
     * Retrieve payment by transaction ID
     */
    public Optional<PaymentResponseDto> getPaymentByTransactionId(String transactionId) {
        log.debug("Fetching payment with transaction id: {}", transactionId);
        return paymentRepository.findByTransactionId(transactionId)
                .map(this::mapToResponseDto);
    }

    /**
     * Create a simulated payment (for testing purposes)
     */
    @Transactional
    public PaymentResponseDto createPayment(PaymentCreateDto dto) {
        log.info("Creating simulated payment for user: {}", dto.getUserId());
        
        User user = findUserById(dto.getUserId());
        Subscription subscription = findSubscriptionById(dto.getSubscriptionId());
        Plan plan = validateAndGetPlan(subscription);
        
        BigDecimal amount = calculateAmount(plan, dto.getBillingCycle());
        
        Payment payment = buildPayment(user, subscription, dto, amount, plan.getCurrency());
        payment.setStatus(Status.COMPLETED);
        payment.setTransactionId(generateSimulatedTransactionId());
        
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Simulated payment created successfully with id: {}", savedPayment.getId());
        
        return mapToResponseDto(savedPayment);
    }

    /**
     * Create PayPal order and return approval URL
     */
    @Transactional
    public String createPayPalOrder(PaymentCreateDto dto) {
        log.info("Creating PayPal order for user: {}", dto.getUserId());
        
        try {
            User user = findUserById(dto.getUserId());
            Subscription subscription = findSubscriptionById(dto.getSubscriptionId());
            Plan plan = validateAndGetPlan(subscription);
            
            BigDecimal amount = calculateAmount(plan, dto.getBillingCycle());
            String currency = plan.getCurrency() != null ? plan.getCurrency() : "USD";
            
            // Create PayPal order request
            OrdersCreateRequest request = buildPayPalOrderRequest(amount, currency, plan.getName());
            
            // Execute PayPal request
            HttpResponse<Order> response = getPayPalClient().execute(request);
            Order order = response.result();
            
            // Save initial payment with PENDING status
            saveInitialPayment(user, subscription, dto, amount, currency, order.id());
            
            // Extract and return approval URL
            String approvalUrl = extractApprovalUrl(order);
            log.info("PayPal order created successfully. Order ID: {}", order.id());
            
            return approvalUrl;
            
        } catch (IOException e) {
            log.error("Error creating PayPal order", e);
            throw new RuntimeException("Failed to create PayPal order: " + e.getMessage(), e);
        }
    }

    /**
     * Capture PayPal payment after user approval
     */
    @Transactional
    public PaymentResponseDto capturePayPalPayment(String orderId) {
        log.info("Capturing PayPal payment for order: {}", orderId);
        
        try {
            OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
            HttpResponse<Order> response = getPayPalClient().execute(request);
            Order order = response.result();
            
            // Update payment status
            Payment payment = paymentRepository.findByTransactionId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
            
            payment.setStatus(Status.COMPLETED);
            payment.setUpdatedAt(LocalDateTime.now());
            
            Payment updatedPayment = paymentRepository.save(payment);
            log.info("PayPal payment captured successfully. Payment ID: {}", updatedPayment.getId());
            
            return mapToResponseDto(updatedPayment);
            
        } catch (IOException e) {
            log.error("Error capturing PayPal payment", e);
            throw new RuntimeException("Failed to capture PayPal payment: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel or refund a payment
     */
    @Transactional
    public PaymentResponseDto cancelPayment(Long paymentId) {
        log.info("Cancelling payment: {}", paymentId);
        
        Payment payment = findPaymentById(paymentId);
        
        if (payment.getStatus() == Status.COMPLETED) {
            payment.setStatus(Status.REFUNDED);
        } else {
            payment.setStatus(Status.CANCELLED);
        }
        
        payment.setUpdatedAt(LocalDateTime.now());
        Payment updatedPayment = paymentRepository.save(payment);
        
        log.info("Payment cancelled/refunded successfully: {}", paymentId);
        return mapToResponseDto(updatedPayment);
    }

    // ==================== Private Helper Methods ====================

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Subscription findSubscriptionById(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + subscriptionId));
    }

    private Payment findPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
    }

    private Plan validateAndGetPlan(Subscription subscription) {
        Plan plan = subscription.getPlan();
        if (plan == null) {
            throw new IllegalStateException("Subscription is not associated with a valid Plan");
        }
        return plan;
    }

    private BigDecimal calculateAmount(Plan plan, String billingCycle) {
        if (billingCycle == null) {
            return Optional.ofNullable(plan.getMonthlyPrice())
                    .orElse(plan.getPrice());
        }
        
        return switch (billingCycle.toLowerCase()) {
            case "annual" -> Optional.ofNullable(plan.getAnnualPrice())
                    .orElse(plan.getPrice());
            case "monthly" -> Optional.ofNullable(plan.getMonthlyPrice())
                    .orElse(plan.getPrice());
            default -> plan.getPrice();
        };
    }

    private Payment buildPayment(User user, Subscription subscription, PaymentCreateDto dto, 
                                  BigDecimal amount, String currency) {
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setSubscription(subscription);
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setAmount(amount);
        payment.setCurrency(currency != null ? currency : "USD");
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }

    private String generateSimulatedTransactionId() {
        return "SIM-" + System.currentTimeMillis();
    }

    private OrdersCreateRequest buildPayPalOrderRequest(BigDecimal amount, String currency, String description) {
        OrdersCreateRequest request = new OrdersCreateRequest();
        request.header("prefer", "return=representation");
        
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");
        
        PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest()
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode(currency)
                        .value(amount.toPlainString()))
                .description(description != null ? description : "Subscription Payment");
        
        orderRequest.purchaseUnits(List.of(purchaseUnitRequest));
        
        ApplicationContext applicationContext = new ApplicationContext()
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .brandName("Converter")
                .landingPage("BILLING")
                .shippingPreference("NO_SHIPPING");
        
        orderRequest.applicationContext(applicationContext);
        request.requestBody(orderRequest);
        
        return request;
    }

    private void saveInitialPayment(User user, Subscription subscription, PaymentCreateDto dto, 
                                    BigDecimal amount, String currency, String orderId) {
        Payment payment = buildPayment(user, subscription, dto, amount, currency);
        payment.setStatus(Status.PENDING);
        payment.setTransactionId(orderId);
        
        paymentRepository.save(payment);
        log.debug("Initial payment saved with PENDING status. Transaction ID: {}", orderId);
    }

    private String extractApprovalUrl(Order order) {
        return order.links().stream()
                .filter(link -> "approve".equals(link.rel()))
                .findFirst()
                .map(LinkDescription::href)
                .orElseThrow(() -> new RuntimeException("Approval URL not found in PayPal response"));
    }

    private PaymentResponseDto mapToResponseDto(Payment payment) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setId(payment.getId());
        dto.setUserId(payment.getUser().getId());
        dto.setSubscriptionId(payment.getSubscription().getId());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setStatus(payment.getStatus());
        dto.setTransactionId(payment.getTransactionId());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        return dto;
    }
}