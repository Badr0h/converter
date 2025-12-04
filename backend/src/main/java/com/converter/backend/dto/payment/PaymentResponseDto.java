package com.converter.backend.dto.payment;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.converter.backend.model.Payment.Status;


@Data
@NoArgsConstructor
public class PaymentResponseDto {
    private Long id ;
    private Long userId ;
    private Long subscriptionId ;
    private BigDecimal amount ; 
    private String currency;
    private Status status ; 
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime createdAt;
    
}
