package com.converter.backend.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.converter.backend.dto.payment.PaymentCreateDto;
import com.converter.backend.dto.payment.PaymentResponseDto;
import com.converter.backend.service.PaymentService;


@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService ; 

    public PaymentController(PaymentService paymentService){
        this.paymentService = paymentService ;
    }


    @GetMapping
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments(){
        return ResponseEntity.ok(paymentService.getAllPayments());
    }


    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> getPaymentById(@PathVariable Long id){
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@RequestBody PaymentCreateDto payment){
        PaymentResponseDto createdPayment = paymentService.createPayment(payment);

        return ResponseEntity
            .created(URI.create("/payment/"+createdPayment.getId()))
            .body(createdPayment);
    }


}
