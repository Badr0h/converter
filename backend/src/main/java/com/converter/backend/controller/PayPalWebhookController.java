package com.converter.backend.controller;

import com.converter.backend.dto.webhook.PayPalWebhookEvent;
import com.converter.backend.service.PayPalWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments/paypal")
@RequiredArgsConstructor
public class PayPalWebhookController {

    private final PayPalWebhookService webhookService;
    private final ObjectMapper objectMapper;

    /**
     * Endpoint pour recevoir les webhooks PayPal
     * PayPal réessayera en cas de réponse 4xx/5xx
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handlePayPalWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "PayPal-Auth-Assertion", required = false) String authAssertion,
            @RequestHeader(value = "PayPal-Transmission-Id", required = false) String transmissionId,
            @RequestHeader(value = "PayPal-Cert-Id", required = false) String certId,
            @RequestHeader(value = "PayPal-Transmission-Sig", required = false) String transmissionSig,
            @RequestHeader(value = "PayPal-Transmission-Time", required = false) String transmissionTime,
            HttpServletRequest request) {

        String requestId = java.util.UUID.randomUUID().toString();
        log.info("[{}] Received PayPal webhook - Transmission ID: {}", requestId, transmissionId);

        try {
            // 1. Valider la signature du webhook
            if (!webhookService.validateWebhookSignature(payload, authAssertion, transmissionId, 
                    certId, transmissionSig, transmissionTime, request)) {
                log.error("[{}] Invalid webhook signature - rejecting", requestId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            // 2. Parser l'événement
            PayPalWebhookEvent event = objectMapper.readValue(payload, PayPalWebhookEvent.class);
            log.info("[{}] Processing webhook event: {} for order: {}", 
                    requestId, event.getEventType(), event.getOrderId());

            // 3. Vérifier l'idempotence (déjà traité ?)
            if (webhookService.isWebhookAlreadyProcessed(event.getId())) {
                log.info("[{}] Webhook already processed: {}", requestId, event.getId());
                return ResponseEntity.ok("Webhook already processed");
            }

            // 4. Traiter l'événement
            webhookService.processWebhookEvent(event, requestId);

            // 5. Enregistrer le webhook comme traité
            webhookService.markWebhookAsProcessed(event, requestId);

            log.info("[{}] Webhook processed successfully: {}", requestId, event.getId());
            return ResponseEntity.ok("Webhook processed successfully");

        } catch (IOException e) {
            log.error("[{}] JSON parsing error for webhook", requestId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid JSON payload");
        } catch (Exception e) {
            log.error("[{}] Error processing webhook", requestId, e);
            // PayPal réessayera avec ce code d'erreur
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error - PayPal will retry");
        }
    }

    /**
     * Endpoint de test pour vérifier que le webhook est accessible
     */
    @GetMapping("/webhook/health")
    public ResponseEntity<Map<String, String>> webhookHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "timestamp", java.time.Instant.now().toString()
        ));
    }
}
