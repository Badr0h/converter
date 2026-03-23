package com.converter.backend.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.converter.backend.dto.payment.PaymentCreateDto;
import com.converter.backend.dto.payment.PaymentResponseDto;
import com.converter.backend.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    /**
     * Endpoint pour créer un paiement simulé (ton code d'origine)
     */
    @PostMapping("/simulate")
    public ResponseEntity<PaymentResponseDto> createSimulatedPayment(@Valid @RequestBody PaymentCreateDto payment) {
        PaymentResponseDto createdPayment = paymentService.createPayment(payment);
        return ResponseEntity
            .created(URI.create("/api/payments/" + createdPayment.getId()))
            .body(createdPayment);
    }

    /**
     * STEP 1: Créer l'ordre PayPal et récupérer l'URL d'approbation
     */
    @PostMapping("/paypal/create")
    public ResponseEntity<Map<String, String>> createPayPalOrder(@Valid @RequestBody PaymentCreateDto dto) {
        log.info("Initiating PayPal order for user: {}", dto.getUserId());
        String approvalUrl = paymentService.createPayPalOrder(dto);
        
        // On renvoie l'URL sous forme de JSON pour le frontend Angular
        return ResponseEntity.ok(Map.of("approvalUrl", approvalUrl));
    }

    /**
     * STEP 2: Capturer le paiement après validation par l'utilisateur sur PayPal
     * L'orderId est envoyé par PayPal au frontend lors du retour (success URL)
     */
    @PostMapping("/paypal/capture/{orderId}")
    public ResponseEntity<PaymentResponseDto> capturePayPalPayment(@PathVariable String orderId) {
        log.info("Capturing PayPal payment for orderId: {}", orderId);
        PaymentResponseDto capturedPayment = paymentService.capturePayPalPayment(orderId);
        return ResponseEntity.ok(capturedPayment);
    }
}