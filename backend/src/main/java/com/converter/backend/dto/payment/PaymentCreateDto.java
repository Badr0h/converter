package com.converter.backend.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new payment.
 * <p>
 * Validation rules are enforced both here (@Valid in controllers) and in
 * PaymentService to prevent price-manipulation attacks.
 *
 * SECURITY NOTE: The actual payment amount is NEVER accepted from the client.
 * It is always computed server-side from the plan stored in the database,
 * preventing any price-manipulation attack.
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
     * The payment method used (e.g., "card", "paypal").
     * Allowed values are validated to prevent injection.
     */
    @NotBlank(message = "Payment method cannot be blank")
    @Pattern(regexp = "^(card|paypal|bank_transfer)$",
             message = "Payment method must be one of: card, paypal, bank_transfer")
    private String paymentMethod;

    /**
     * A secure, single-use token from the payment provider (e.g., Stripe, PayPal).
     * This token authorises the charge on the provider's side and is NEVER stored.
     * Optional for PayPal flows where the token comes from the redirect callback.
     */
    private String paymentToken;

    /**
     * Optional billing cycle override: 'monthly' or 'annual'.
     * When provided, PaymentService uses the corresponding plan price
     * (plan.monthlyPrice / plan.annualPrice).
     * Any other value is rejected to prevent unexpected behaviour.
     */
    @Pattern(regexp = "^(monthly|annual)?$",
             message = "Billing cycle must be 'monthly' or 'annual'")
    private String billingCycle;
}