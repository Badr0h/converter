package com.converter.backend.dto.subscription;

import com.converter.backend.model.Subscription.SubscriptionDuration;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating a new subscription.
 */
@Data
@NoArgsConstructor
public class SubscriptionCreateDto {

    /**
     * The start date of the subscription. Must be today or in the future.
     */
    @NotNull(message = "Start date cannot be null")
    @FutureOrPresent(message = "Start date must be in the present or future")
    private LocalDate startDate;

    /**
     * The duration of the subscription (e.g., ONE_MONTH, THREE_MONTHS).
     */
    @NotNull(message = "Subscription duration cannot be null")
    private SubscriptionDuration duration;

    /**
     * The ID of the plan to which the user is subscribing.
     */
    @NotNull(message = "Plan ID cannot be null")
    private Long planId;
}
