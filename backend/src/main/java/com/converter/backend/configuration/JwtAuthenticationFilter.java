package com.converter.backend.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("=== JWT AUTH FILTER DEBUG ===");
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Request Method: {}", request.getMethod());
        
        final String authHeader = request.getHeader("Authorization");
        log.info("Authorization header: {}", authHeader);
        
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("❌ No JWT token found or invalid format");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        log.info("JWT token extracted (length: {})", jwt.length());
        log.info("JWT preview: {}...", jwt.substring(0, Math.min(50, jwt.length())));

        try {
            userEmail = jwtService.extractUsername(jwt);
            log.info("Extracted username from JWT: {}", userEmail);
            
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                log.info("UserDetails loaded - Username: {}, Enabled: {}, Authorities: {}", 
                    userDetails.getUsername(), userDetails.isEnabled(), userDetails.getAuthorities());
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("✅ JWT token validated and authentication set");
                } else {
                    log.warn("❌ JWT token validation failed");
                }
            } else {
                log.warn("❌ Username is null or authentication already exists");
            }
        } catch (Exception e) {
            log.error("❌ Error processing JWT token: {}", e.getMessage(), e);
        }
        
        log.info("==========================");
        
        filterChain.doFilter(request, response);
    }
}
