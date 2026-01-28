package com.converter.backend.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastResetTime = new ConcurrentHashMap<>();
    
    // Rate limits: endpoint -> (maxRequests, windowMinutes)
    private static final ConcurrentHashMap<String, int[]> RATE_LIMITS = new ConcurrentHashMap<>();
    
    static {
        RATE_LIMITS.put("/api/auth/login", new int[]{5, 15}); // 5 attempts per 15 minutes
        RATE_LIMITS.put("/api/auth/register", new int[]{3, 60}); // 3 registrations per hour
        RATE_LIMITS.put("/api/conversions", new int[]{20, 10}); // 20 conversions per 10 minutes
        RATE_LIMITS.put("/api/auth/resend-verification", new int[]{2, 5}); // 2 emails per 5 minutes (anti-spam)
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                  @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();
        String key = clientIp + ":" + requestUri;
        
        // Check if this endpoint has rate limiting
        int[] limits = getRateLimitForEndpoint(requestUri);
        if (limits != null) {
            int maxRequests = limits[0];
            int windowMinutes = limits[1];
            
            // Reset counter if window has expired
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastReset = lastResetTime.get(key);
            
            if (lastReset == null || lastReset.plus(windowMinutes, ChronoUnit.MINUTES).isBefore(now)) {
                requestCounts.put(key, new AtomicInteger(0));
                lastResetTime.put(key, now);
            }
            
            // Check rate limit
            AtomicInteger count = requestCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
            if (count.incrementAndGet() > maxRequests) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write(String.format(
                    "{\"error\":\"Rate limit exceeded\",\"message\":\"Maximum %d requests per %d minutes allowed\",\"retryAfter\":%d}",
                    maxRequests, windowMinutes, windowMinutes * 60
                ));
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private int[] getRateLimitForEndpoint(String requestUri) {
        // Exact match first
        if (RATE_LIMITS.containsKey(requestUri)) {
            return RATE_LIMITS.get(requestUri);
        }
        
        // Prefix match for dynamic endpoints
        for (String endpoint : RATE_LIMITS.keySet()) {
            if (requestUri.startsWith(endpoint)) {
                return RATE_LIMITS.get(endpoint);
            }
        }
        
        return null;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
