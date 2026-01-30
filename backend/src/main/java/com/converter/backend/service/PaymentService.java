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
import com.converter.backend.repository.ProcessedWebhookRepository;
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
    private final ProcessedWebhookRepository processedWebhookRepository;
    
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
        payment.setStatus(Status.SUCCESS);
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
     * Capture PayPal payment after user approval (AVEC IDEMPOTENCE ET ACTIVATION ATOMIQUE)
     */
    @Transactional
    public PaymentResponseDto capturePayPalPayment(String orderId) {
        return capturePayPalPayment(orderId, "frontend-capture");
    }

    /**
     * Capture PayPal payment avec idempotence et activation atomique
     */
    @Transactional
    public PaymentResponseDto capturePayPalPayment(String orderId, String source) {
        log.info("Capturing PayPal payment for order: {} from source: {}", orderId, source);
        
        try {
            // 1. Vérifier l'idempotence - déjà capturé ?
            Optional<Payment> existingPayment = paymentRepository.findByTransactionId(orderId);
            if (existingPayment.isPresent() && existingPayment.get().getStatus() == Status.SUCCESS) {
                log.info("Payment already captured for order: {} - returning existing", orderId);
                return mapToResponseDto(existingPayment.get());
            }

            // 2. Valider le statut de l'ordre PayPal
            validatePayPalOrderStatus(orderId);

            // 3. Capturer le paiement PayPal
            OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
            request.requestBody(new OrderRequest());
            HttpResponse<Order> response = getPayPalClient().execute(request);
            Order order = response.result();

            if (!"COMPLETED".equals(order.status())) {
                throw new IllegalStateException("PayPal order not completed. Status: " + order.status());
            }

            // 4. Trouver et mettre à jour le paiement
            Payment payment = paymentRepository.findByTransactionId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));

            // 5. Mettre à jour le statut du paiement
            payment.setStatus(Status.SUCCESS);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // 6. ACTIVATION ATOMIQUE DE L'ABONNEMENT DANS LA MÊME TRANSACTION
            activateSubscriptionAtomically(payment.getSubscription(), source);

            log.info("PayPal payment captured and subscription activated successfully. Payment ID: {}, Subscription ID: {}", 
                    payment.getId(), payment.getSubscription().getId());

            return mapToResponseDto(payment);

        } catch (IOException e) {
            log.error("Error capturing PayPal payment for order: {}", orderId, e);
            throw new RuntimeException("Failed to capture PayPal payment: " + e.getMessage(), e);
        }
    }

    /**
     * Gère l'événement PayPal ORDER.APPROVED (webhook)
     */
    @Transactional
    public void handlePayPalOrderApproved(String orderId, String requestId) {
        log.info("[{}] Handling ORDER.APPROVED for order: {}", requestId, orderId);
        
        try {
            // Pour ORDER.APPROVED, on ne fait rien de spécial
            // On attend la capture pour éviter les abandons de paiement
            log.info("[{}] ORDER.APPROVED received - waiting for CAPTURE.COMPLETED", requestId);
            
        } catch (Exception e) {
            log.error("[{}] Error handling ORDER.APPROVED for order: {}", requestId, orderId, e);
            throw new RuntimeException("Failed to handle ORDER.APPROVED", e);
        }
    }

    /**
     * Gère l'événement PayPal PAYMENT.CAPTURE.COMPLETED (webhook)
     * C'est la méthode principale qui garantit l'atomicité
     */
    @Transactional
    public void handlePayPalPaymentCaptured(String orderId, String requestId) {
        log.info("[{}] Handling PAYMENT.CAPTURE.COMPLETED for order: {}", requestId, orderId);
        
        try {
            // Utiliser la même logique que capturePayPalPayment mais pour le webhook
            capturePayPalPayment(orderId, "webhook-" + requestId);
            log.info("[{}] PAYMENT.CAPTURE.COMPLETED processed successfully", requestId);
            
        } catch (Exception e) {
            log.error("[{}] Error handling PAYMENT.CAPTURE.COMPLETED for order: {}", requestId, orderId, e);
            throw new RuntimeException("Failed to handle PAYMENT.CAPTURE.COMPLETED", e);
        }
    }

    /**
     * Gère l'événement PayPal PAYMENT.CAPTURE.DENIED (webhook)
     */
    @Transactional
    public void handlePayPalPaymentDenied(String orderId, String requestId) {
        log.warn("[{}] Handling PAYMENT.CAPTURE.DENIED for order: {}", requestId, orderId);
        
        try {
            Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(orderId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setStatus(Status.FAILED);
                payment.setFailureReason("PayPal payment denied");
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                
                log.info("[{}] Payment marked as FAILED for order: {}", requestId, orderId);
            } else {
                log.warn("[{}] Payment not found for denied order: {}", requestId, orderId);
            }
            
        } catch (Exception e) {
            log.error("[{}] Error handling PAYMENT.CAPTURE.DENIED for order: {}", requestId, orderId, e);
            throw new RuntimeException("Failed to handle PAYMENT.CAPTURE.DENIED", e);
        }
    }

    /**
     * Cancel or refund a payment
     */
    @Transactional
    public PaymentResponseDto cancelPayment(Long paymentId) {
        log.info("Cancelling payment: {}", paymentId);
        
        Payment payment = findPaymentById(paymentId);
        
        if (payment.getStatus() == Status.SUCCESS) {
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

    /**
     * Valide le statut de l'ordre PayPal avant capture
     */
    private void validatePayPalOrderStatus(String orderId) throws IOException {
        OrdersGetRequest request = new OrdersGetRequest(orderId);
        HttpResponse<Order> response = getPayPalClient().execute(request);
        Order order = response.result();
        
        if (!"APPROVED".equals(order.status())) {
            throw new IllegalStateException("PayPal order not approved. Current status: " + order.status());
        }
        
        log.debug("PayPal order {} validated with status: {}", orderId, order.status());
    }

    /**
     * Active l'abonnement de manière atomique après paiement réussi
     */
    private void activateSubscriptionAtomically(Subscription subscription, String source) {
        log.info("Activating subscription {} atomically from source: {}", subscription.getId(), source);
        
        if (subscription.getStatus() == Subscription.Status.ACTIVE) {
            log.info("Subscription {} already active - skipping activation", subscription.getId());
            return;
        }
        
        // Mettre à jour le statut de l'abonnement
        subscription.setStatus(Subscription.Status.ACTIVE);
        
        // S'assurer que les conversions maximales sont définies
        if (subscription.getPlan() != null && subscription.getMaxConversionsPerMonth() == null) {
            subscription.setMaxConversionsPerMonth(subscription.getPlan().getMaxConversions());
        }
        
        subscriptionRepository.save(subscription);
        
        log.info("Subscription {} activated successfully from source: {}", subscription.getId(), source);
    }
}