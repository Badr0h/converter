package com.converter.backend.dto.subscription;

import com.converter.backend.model.Subscription.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating the status of a subscription.
 */
@Data
@NoArgsConstructor
public class SubscriptionUpdateDto {

    /**
     * The new status for the subscription (e.g., ACTIVE, CANCELLED).
     */
    @NotNull(message = "Subscription status cannot be null")
    private Status status;
}
