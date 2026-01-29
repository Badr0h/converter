package com.converter.backend.repository;

import com.converter.backend.model.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, Long> {

    /**
     * Find processed webhook by PayPal event ID
     */
    Optional<ProcessedWebhook> findByWebhookEventId(String webhookEventId);

    /**
     * Check if webhook event was already processed
     */
    boolean existsByWebhookEventId(String webhookEventId);

    /**
     * Find processed webhook by order ID
     */
    Optional<ProcessedWebhook> findByOrderId(String orderId);
}
