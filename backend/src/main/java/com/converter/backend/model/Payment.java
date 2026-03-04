package com.converter.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_subscription_id", columnList = "subscription_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_payment_created_at", columnList = "created_at")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_subscription"))
    private Subscription subscription;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @Column(name = "payment_gateway", length = 50)
    private String paymentGateway; // e.g., "PAYPAL", "STRIPE", "MANUAL"

    @Column(name = "billing_cycle", length = 20)
    private String billingCycle; // "MONTHLY", "ANNUAL"

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = Status.PENDING;
        }
        if (currency == null) {
            currency = "USD";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business logic methods
    public boolean isSuccess() {
        return Status.SUCCESS.equals(status);
    }

    public boolean isPending() {
        return Status.PENDING.equals(status);
    }

    public boolean isFailed() {
        return Status.FAILED.equals(status);
    }

    public boolean isRefunded() {
        return Status.REFUNDED.equals(status);
    }

    public boolean isCancelled() {
        return Status.CANCELLED.equals(status);
    }

    public void markAsCompleted(String transactionId) {
        this.status = Status.SUCCESS;
        this.transactionId = transactionId;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.status = Status.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsRefunded(BigDecimal refundAmount) {
        this.status = Status.REFUNDED;
        this.refundAmount = refundAmount;
        this.refundDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsPartiallyRefunded(BigDecimal refundAmount) {
        this.status = Status.PARTIALLY_REFUNDED;
        this.refundAmount = refundAmount;
        this.refundDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCancelled() {
        this.status = Status.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING,                // Payment initiated but not completed
        SUCCESS,                // Payment successful (matches SQL V1)
        FAILED,                 // Payment failed
        REFUNDED,               // Payment refunded
        PARTIALLY_REFUNDED,     // Payment partially refunded (matches SQL V1)
        CANCELLED               // Payment cancelled by user
    }

    public enum PaymentGateway {
        PAYPAL,
        STRIPE,
        CREDIT_CARD,
        BANK_TRANSFER,
        MANUAL,
        OTHER
    }

    public enum BillingCycle {
        MONTHLY,
        ANNUAL,
        QUARTERLY,
        ONE_TIME
    }
}