package com.converter.backend.dto.subscription;

import com.converter.backend.model.Subscription.Status;
import com.converter.backend.model.Subscription.SubscriptionDuration;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
public class SubscriptionResponseDto {

    private Long id ; 
    private Status status ;
    private SubscriptionDuration duration ; 
    private LocalDate startDate ; 
    private LocalDate endDate ; 
    private LocalDateTime createdAt ; 
    private String planName;
    private Long planId;
    private BigDecimal price;
    private Integer maxConversionsPerMonth; // Conversions allowed per month
}
