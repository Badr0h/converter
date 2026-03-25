package com.converter.backend.service;

import com.converter.backend.dto.auth.AuthRequest;
import com.converter.backend.configuration.JwtService;
import com.converter.backend.dto.auth.AuthResponse;
import com.converter.backend.dto.user.UserCreateDto;
import com.converter.backend.dto.user.UserResponseDto;
import com.converter.backend.model.User;
import com.converter.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthResponse register(UserCreateDto request) {
        // Check if user already exists
        var existingUser = userRepository.findByEmail(request.getEmail());
        
        User user;
        if (existingUser.isPresent()) {
            // If user exists but is not enabled, update their verification code
            user = existingUser.get();
            if (!user.isEnabled()) {
                String verificationCode = emailService.generateVerificationCode();
                user.setVerificationCode(verificationCode);
                user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(15));
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                emailService.sendVerificationEmail(user.getEmail(), verificationCode);
                
                UserResponseDto userDto = mapUserToDto(user);
                return AuthResponse.builder()
                        .token(null)
                        .user(userDto)
                        .refreshToken(null)
                        .build();
            } else {
                // User already exists and is enabled - return error response
                throw new RuntimeException("Un compte avec cet email existe déjà");
            }
        }
        
        // Create new user
        user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.USER);
        
        String verificationCode = emailService.generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);
        
        userRepository.save(user);
        
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);
        
        UserResponseDto userDto = mapUserToDto(user);
        return AuthResponse.builder()
                .token(null)
                .user(userDto)
                .refreshToken(null)
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        UserResponseDto userDto = mapUserToDto(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .user(userDto)
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }

    private UserResponseDto mapUserToDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto ;
        // Implement the mapping logic from User to UserResponseDto
    }


    public AuthResponse refreshToken(String refreshToken) {
        // Validate refresh token
        String email = jwtService.extractUsername(refreshToken);
        var user = userRepository.findByEmail(email).orElseThrow();
        
        var newJwtToken = jwtService.generateToken(user);
        var newRefreshToken = jwtService.generateRefreshToken(user);
        var userDto = mapUserToDto(user);
        
        return AuthResponse.builder()
                .token(newJwtToken)
                .refreshToken(newRefreshToken)
                .user(userDto)
                .build();
    }

    public boolean verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.getVerificationCode() == null || user.getVerificationCodeExpiry() == null) {
            return false;
        }

        if (user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        if (!user.getVerificationCode().equals(code)) {
            return false;
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);

        return true;
    }


    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String verificationCode = emailService.generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(15));
        
        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur avec cet email non trouvé"));

        // Generate reset token (using a simple UUID-based token)
        String resetToken = java.util.UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1)); // Token valid for 1 hour
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        // Send reset email with token
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    public boolean resetPassword(String resetToken, String newPassword) {
        User user = userRepository.findAll().stream()
                .filter(u -> resetToken.equals(u.getResetToken()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Token de réinitialisation invalide"));

        // Check if token has expired
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Update password and clear reset token
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        return true;
    }
}