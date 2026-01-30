package com.converter.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "processed_webhooks", 
    indexes = {
        @Index(name = "idx_webhook_event_id", columnList = "webhook_event_id"),
        @Index(name = "idx_webhook_order_id", columnList = "order_id"),
        @Index(name = "idx_webhook_processed_at", columnList = "processed_at")
    })
public class ProcessedWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "webhook_event_id", unique = true, nullable = false, length = 100)
    private String webhookEventId;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessingStatus status = ProcessingStatus.PROCESSED;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "payload", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private Map<String, Object> payload;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    public enum ProcessingStatus {
        PROCESSED,
        FAILED,
        DUPLICATE
    }
}
