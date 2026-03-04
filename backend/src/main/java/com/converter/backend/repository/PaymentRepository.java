package com.converter.backend.repository;

import com.converter.backend.model.Payment;
import com.converter.backend.model.Payment.Status;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"user", "subscription"})
    @Override
    List<Payment> findAll();

    /**
     * Find payment by transaction ID
     */
    @EntityGraph(attributePaths = {"user", "subscription"})
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Find all payments for a specific user
     */
    @EntityGraph(attributePaths = {"subscription"})
    List<Payment> findByUserId(Long userId);

    /**
     * Find all payments for a specific subscription
     */
    @EntityGraph(attributePaths = {"user"})
    List<Payment> findBySubscriptionId(Long subscriptionId);

    /**
     * Find payments by status
     */
    @EntityGraph(attributePaths = {"user", "subscription"})
    List<Payment> findByStatus(Status status);

    /**
     * Find payments by user and status
     */
    @EntityGraph(attributePaths = {"subscription"})
    List<Payment> findByUserIdAndStatus(Long userId, Status status);

    /**
     * Find payments created between two dates
     */
    @EntityGraph(attributePaths = {"user", "subscription"})
    List<Payment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find pending payments older than a specific date (for cleanup)
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :expiryDate")
    List<Payment> findExpiredPendingPayments(@Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Get total amount paid by user
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.user.id = :userId AND p.status = 'COMPLETED'")
    Double getTotalAmountByUserId(@Param("userId") Long userId);

    /**
     * Count payments by status
     */
    long countByStatus(Status status);

    /**
     * Check if user has any completed payment
     */
    boolean existsByUserIdAndStatus(Long userId, Status status);
}