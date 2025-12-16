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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(UserCreateDto request) {
        var user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.USER);
        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        UserResponseDto userDto = mapUserToDto(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .user(userDto)
                .refreshToken(jwtService.generateRefreshToken(user))
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
}