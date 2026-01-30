package com.converter.backend.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import com.converter.backend.repository.PaymentRepository;
import com.converter.backend.model.Payment.Status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Configuration
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class PaymentCleanupConfiguration {

    private final PaymentRepository paymentRepository;

    /**
     * Nettoie les paiements PENDING expirés toutes les heures
     * Un paiement PENDING de plus de 30 minutes est considéré comme expiré
     */
    @Scheduled(fixedRate = 3600000) // 1 heure en millisecondes
    @Transactional
    public void cleanupExpiredPendingPayments() {
        log.info("Starting cleanup of expired pending payments");
        
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(30);
        var expiredPayments = paymentRepository.findExpiredPendingPayments(expiryTime);
        
        if (!expiredPayments.isEmpty()) {
            expiredPayments.forEach(payment -> {
                payment.setStatus(Status.CANCELLED);
                payment.setFailureReason("Payment expired - not completed within 30 minutes");
            });
            
            paymentRepository.saveAll(expiredPayments);
            log.info("Cleaned up {} expired pending payments", expiredPayments.size());
        } else {
            log.debug("No expired pending payments found");
        }
    }

    /**
     * Rapport quotidien sur les statistiques de paiement
     */
    @Scheduled(cron = "0 0 1 * * ?") // Tous les jours à 1h du matin
    @Transactional(readOnly = true)
    public void dailyPaymentReport() {
        log.info("Generating daily payment report");
        
        long completedCount = paymentRepository.countByStatus(Status.SUCCESS);
        long pendingCount = paymentRepository.countByStatus(Status.PENDING);
        long failedCount = paymentRepository.countByStatus(Status.FAILED);
        
        log.info("Daily Payment Report - COMPLETED: {}, PENDING: {}, FAILED: {}", 
                completedCount, pendingCount, failedCount);
    }
}
