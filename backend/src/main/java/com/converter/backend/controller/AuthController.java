package com.converter.backend.controller;

import com.converter.backend.dto.auth.AuthRequest;
import com.converter.backend.dto.auth.AuthResponse;
import com.converter.backend.dto.auth.RefreshTokenRequest;
import com.converter.backend.dto.user.UserCreateDto;
import com.converter.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserCreateDto request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        
        boolean isVerified = authService.verifyEmail(email, code);
        
        if (isVerified) {
            return ResponseEntity.ok(Map.of("message", "Email vérifié avec succès"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Code de vérification invalide ou expiré"));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        authService.resendVerificationCode(email);
        return ResponseEntity.ok(Map.of("message", "Code de vérification renvoyé"));
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        authService.requestPasswordReset(email);
        return ResponseEntity.ok(Map.of("message", "Un email de réinitialisation a été envoyé"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String resetToken = request.get("token");
        String newPassword = request.get("password");
        
        boolean isReset = authService.resetPassword(resetToken, newPassword);
        
        if (isReset) {
            return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Token de réinitialisation invalide ou expiré"));
        }
    }
}
