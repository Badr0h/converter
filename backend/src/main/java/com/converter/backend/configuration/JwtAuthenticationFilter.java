package com.converter.backend.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("JWT Filter processing request: {}", request.getRequestURI());
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        log.debug("Processing request to: {}", request.getRequestURI());
        log.debug("Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("No valid Authorization header found");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        log.debug("Extracted JWT token: {}", jwt.substring(0, Math.min(jwt.length(), 20)) + "...");
        
        try {
            userEmail = jwtService.extractUsername(jwt);
            log.debug("Extracted username from JWT: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                log.debug("Loaded user details for: {}", userDetails.getUsername());
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Successfully authenticated user: {}", userEmail);
                } else {
                    log.warn("JWT token validation failed for user: {}", userEmail);
                }
            }
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage(), e);
        }
        
        filterChain.doFilter(request, response);
    }
}
