package com.converter.backend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class PayPalWebhookEvent {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("event_version")
    private String eventVersion;
    
    @JsonProperty("create_time")
    private LocalDateTime createTime;
    
    @JsonProperty("resource_type")
    private String resourceType;
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("resource")
    private Map<String, Object> resource;
    
    @JsonProperty("links")
    private Object[] links;
    
    @JsonProperty("status")
    private String status;
    
    // Méthodes utilitaires pour vérifier les types d'événements
    public boolean isOrderApproved() {
        return "CHECKOUT.ORDER.APPROVED".equals(eventType);
    }
    
    public boolean isPaymentCaptured() {
        return "PAYMENT.CAPTURE.COMPLETED".equals(eventType);
    }
    
    public boolean isPaymentCaptureDenied() {
        return "PAYMENT.CAPTURE.DENIED".equals(eventType);
    }
    
    public String getOrderId() {
        if (resource != null && resource.containsKey("id")) {
            return (String) resource.get("id");
        }
        return null;
    }
    
    public String getCaptureId() {
        if (resource != null && resource.containsKey("id")) {
            return (String) resource.get("id");
        }
        return null;
    }
    
    public String getStatus() {
        if (resource != null && resource.containsKey("status")) {
            return (String) resource.get("status");
        }
        return null;
    }
}
