package com.converter.backend.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new payment.
 * This object links a user to a subscription and includes the payment details.
 */
@Data
@NoArgsConstructor
public class PaymentCreateDto {

    /**
     * The ID of the user initiating the payment.
     */
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    /**
     * The ID of the subscription being paid for.
     */
    @NotNull(message = "Subscription ID cannot be null")
    private Long subscriptionId;

    /**
     * The payment method used (e.g., "Card", "PayPal").
     */
    @NotBlank(message = "Payment method cannot be blank")
    private String paymentMethod;

    /**
     * A secure, single-use token from the payment provider (e.g., Stripe, PayPal)
     * that authorizes the payment. This should never be stored.
     */
    @NotBlank(message = "Payment token cannot be blank")
    private String paymentToken;
    /**
     * Optional billing cycle: 'monthly' or 'annual'. When provided, PaymentService
     * will use the corresponding plan price (monthlyPrice/annualPrice) if available.
     */
    private String billingCycle;
}