package com.converter.backend.dto.payment;

import com.converter.backend.model.Payment.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    
    private Long id;
    
    private Long userId;
    
    private Long subscriptionId;
    
    private BigDecimal amount;
    
    private String currency;
    
    private String paymentMethod;
    
    private Status status;
    
    private String transactionId;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}