package com.converter.backend.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Base64;

// @Component
public class JwtValidator {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    private static final int MIN_SECRET_LENGTH_BYTES = 32; // 256 bits
    
    @PostConstruct
    public void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret cannot be null or empty");
        }
        
        // Decode Base64 to check actual byte length
        byte[] secretBytes;
        try {
            secretBytes = Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException e) {
            // If not Base64 encoded, treat as raw string
            secretBytes = jwtSecret.getBytes();
        }
        
        if (secretBytes.length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException(
                String.format("JWT secret must be at least %d bytes (256 bits) long. Current length: %d bytes", 
                MIN_SECRET_LENGTH_BYTES, secretBytes.length)
            );
        }
        
        // Check for weak/default secrets
        if (isWeakSecret(jwtSecret)) {
            throw new IllegalStateException("JWT secret appears to be a weak or default value. Please use a strong, randomly generated secret.");
        }
    }
    
    private boolean isWeakSecret(String secret) {
        String lowerSecret = secret.toLowerCase();
        return lowerSecret.contains("secret") || 
               lowerSecret.contains("change") ||
               lowerSecret.contains("default") ||
               lowerSecret.contains("test") ||
               secret.length() < 20;
    }
}
