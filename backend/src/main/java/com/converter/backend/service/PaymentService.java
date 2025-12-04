package com.converter.backend.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import com.converter.backend.dto.payment.PaymentCreateDto;
import com.converter.backend.dto.payment.PaymentResponseDto;
import com.converter.backend.exception.ResourceNotFoundException;
import com.converter.backend.model.Payment;
import com.converter.backend.model.Plan;
import com.converter.backend.model.Subscription;
import com.converter.backend.model.User;
import com.converter.backend.model.Payment.Status;
import com.converter.backend.repository.PaymentRepository;
import com.converter.backend.repository.PlanRepository;
import com.converter.backend.repository.SubscriptionRepository;
import com.converter.backend.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository ;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository ; 
    private final UserRepository userRepository ; 
   // private final PaymentGatewayClient gatewayClient; // Interface pour Stripe/PayPal

    public PaymentService(PaymentRepository paymentRepository,SubscriptionRepository subscriptionRepository,PlanRepository planRepository,UserRepository userRepository){
        this.paymentRepository = paymentRepository ; 
        this.subscriptionRepository = subscriptionRepository ; 
        this.planRepository = planRepository ; 
        this.userRepository = userRepository ; 
        
    }


    public List<PaymentResponseDto> getAllPayments(){
        return paymentRepository.findAll()
            .stream()
            .map(this::mapToPaymentResponseDto)
            .toList();
    }
    public PaymentResponseDto getPaymentById(Long id){
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(()-> new ResourceNotFoundException("payement not found with id : " + id));

        return mapToPaymentResponseDto(payment);
    }

    @Transactional
    public PaymentResponseDto createPayment(PaymentCreateDto dto){
        User user = userRepository.findById(dto.getUserId())
            .orElseThrow(()-> new ResourceNotFoundException("user not found with id : " + dto.getUserId() ));

        Subscription subscription = subscriptionRepository.findById(dto.getSubscriptionId())
            .orElseThrow(()-> new ResourceNotFoundException("subscription not found with id : " + dto.getSubscriptionId() ));
        
        Plan plan = subscription.getPlan();

        if (plan == null) {
            throw new IllegalStateException("Subscription is not associated with a valid Plan.");
        }
        BigDecimal amount = plan.getPrice(); 
        String currency = plan.getCurrency();

    Payment payment = new Payment();
    payment.setUser(user);
    payment.setSubscription(subscription);
    payment.setPaymentMethod(dto.getPaymentMethod());
    payment.setAmount(amount);
    payment.setCurrency(currency);
    payment.setStatus(Status.COMPLETED); // Simuler un paiement réussi
    payment.setTransactionId("SIM-" + System.currentTimeMillis()); // ID fictif
    payment.setCreatedAt(LocalDateTime.now());

    Payment savedPayment = paymentRepository.save(payment);

    return mapToPaymentResponseDto(savedPayment);
   }
    
    

    private  PaymentResponseDto mapToPaymentResponseDto(Payment payment){
        PaymentResponseDto dto = new PaymentResponseDto(); 
        dto.setId(payment.getId());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setStatus(payment.getStatus());
        dto.setCreatedAt(payment.getCreatedAt());

        return dto ; 
    }
}
