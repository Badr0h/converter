package com.converter.backend.service;

import com.converter.backend.dto.webhook.PayPalWebhookEvent;
import com.converter.backend.model.ProcessedWebhook;
import com.converter.backend.repository.ProcessedWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalWebhookService {

    private final ProcessedWebhookRepository processedWebhookRepository;
    private final PaymentService paymentService;

    @Value("${paypal.webhook.id:}")
    private String webhookId;

    @Value("${paypal.client.secret}")
    private String paypalClientSecret;

    /**
     * Valide la signature du webhook PayPal
     */
    public boolean validateWebhookSignature(String payload, String authAssertion, String transmissionId,
                                          String certId, String transmissionSig, String transmissionTime,
                                          HttpServletRequest request) {
        
        try {
            // Pour le développement, on peut bypasser la validation
            if (webhookId.isEmpty() || "sandbox".equals(System.getProperty("paypal.mode", "sandbox"))) {
                log.debug("Webhook signature validation bypassed for sandbox mode");
                return true;
            }

            // Construction de la chaîne de vérification selon la documentation PayPal
            String verificationString = String.format(
                "%s|%s|%s|%s|%s",
                transmissionId,
                transmissionTime,
                webhookId,
                certId,
                payload
            );

            // Calcul de la signature HMAC-SHA256
            String expectedSignature = calculateHMACSHA256(verificationString, paypalClientSecret);
            
            // Comparaison avec la signature reçue
            boolean isValid = expectedSignature.equals(transmissionSig);
            
            if (!isValid) {
                log.warn("Webhook signature validation failed. Expected: {}, Received: {}", 
                        expectedSignature, transmissionSig);
            }
            
            return isValid;

        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            // En cas d'erreur de validation, on rejette par sécurité
            return false;
        }
    }

    /**
     * Vérifie si le webhook a déjà été traité (idempotence)
     */
    public boolean isWebhookAlreadyProcessed(String webhookEventId) {
        return processedWebhookRepository.existsByWebhookEventId(webhookEventId);
    }

    /**
     * Traite l'événement webhook
     */
    public void processWebhookEvent(PayPalWebhookEvent event, String requestId) {
        try {
            if (event.isOrderApproved()) {
                log.info("[{}] Processing ORDER.APPROVED for order: {}", requestId, event.getOrderId());
                // Pour ORDER.APPROVED, on attend la capture pour éviter les paiements abandonnés
                paymentService.handlePayPalOrderApproved(event.getOrderId(), requestId);
                
            } else if (event.isPaymentCaptured()) {
                log.info("[{}] Processing PAYMENT.CAPTURE.COMPLETED for order: {}", requestId, event.getOrderId());
                // C'est ici que l'activation atomique se produit
                paymentService.handlePayPalPaymentCaptured(event.getOrderId(), requestId);
                
            } else if (event.isPaymentCaptureDenied()) {
                log.warn("[{}] Processing PAYMENT.CAPTURE.DENIED for order: {}", requestId, event.getOrderId());
                paymentService.handlePayPalPaymentDenied(event.getOrderId(), requestId);
                
            } else {
                log.info("[{}] Unhandled webhook event type: {}", requestId, event.getEventType());
            }
            
        } catch (Exception e) {
            log.error("[{}] Error processing webhook event: {}", requestId, event.getEventType(), e);
            throw new RuntimeException("Failed to process webhook event: " + event.getEventType(), e);
        }
    }

    /**
     * Marque le webhook comme traité dans la base de données
     */
    public void markWebhookAsProcessed(PayPalWebhookEvent event, String requestId) {
        try {
            ProcessedWebhook processedWebhook = ProcessedWebhook.builder()
                    .webhookEventId(event.getId())
                    .orderId(event.getOrderId())
                    .eventType(event.getEventType())
                    .status(ProcessedWebhook.ProcessingStatus.PROCESSED)
                    .payload(Map.of(
                        "id", event.getId(),
                        "orderId", event.getOrderId(),
                        "eventType", event.getEventType(),
                        "summary", event.getSummary(),
                        "resource", event.getResource(),
                        "status", event.getStatus(),
                        "createTime", event.getCreateTime()
                    ))
                    .build();
            
            processedWebhookRepository.save(processedWebhook);
            log.debug("[{}] Webhook marked as processed: {}", requestId, event.getId());
            
        } catch (Exception e) {
            log.error("[{}] Error marking webhook as processed: {}", requestId, event.getId(), e);
            // On ne lève pas d'exception ici car le traitement principal a réussi
        }
    }

    /**
     * Marque un webhook comme échoué
     */
    public void markWebhookAsFailed(String webhookEventId, String orderId, String eventType, 
                                   String errorMessage, String requestId) {
        try {
            ProcessedWebhook processedWebhook = ProcessedWebhook.builder()
                    .webhookEventId(webhookEventId)
                    .orderId(orderId)
                    .eventType(eventType)
                    .status(ProcessedWebhook.ProcessingStatus.FAILED)
                    .errorMessage(errorMessage)
                    .build();
            
            processedWebhookRepository.save(processedWebhook);
            log.debug("[{}] Webhook marked as failed: {}", requestId, webhookEventId);
            
        } catch (Exception e) {
            log.error("[{}] Error marking webhook as failed: {}", requestId, webhookEventId, e);
        }
    }

    /**
     * Calcule la signature HMAC-SHA256
     */
    private String calculateHMACSHA256(String data, String key) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] signedBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signedBytes);
    }
}
