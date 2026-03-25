package com.converter.backend.controller;

import com.converter.backend.dto.conversion.ConversionResponseDto;
import com.converter.backend.dto.user.UserCreateDto;
import com.converter.backend.dto.user.UserResponseDto;
import com.converter.backend.dto.user.UserUpdateDto;
import com.converter.backend.service.ConversionService;
import com.converter.backend.service.UserService;
import com.converter.backend.repository.UserRepository;
import com.converter.backend.repository.ConversionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final ConversionService conversionService;
    private final UserRepository userRepository;
    private final ConversionRepository conversionRepository;

    // ===== DASHBOARD STATS =====

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalConversions = conversionRepository.count();

        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        long conversionsToday = conversionRepository.countByCreatedAtAfter(startOfToday);

        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        long activeUsersThisWeek = conversionRepository.countDistinctUsersSince(startOfWeek);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalConversions", totalConversions);
        stats.put("conversionsToday", conversionsToday);
        stats.put("activeUsersThisWeek", activeUsersThisWeek);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/conversions-per-day")
    public ResponseEntity<List<Map<String, Object>>> getConversionsPerDay() {
        List<Map<String, Object>> result = conversionRepository.countByDayLast7Days();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/recent-activity")
    public ResponseEntity<List<ConversionResponseDto>> getRecentActivity() {
        List<ConversionResponseDto> recent = conversionService.getRecentActivity(10);
        return ResponseEntity.ok(recent);
    }

    // ===== USER MANAGEMENT =====

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @RequestBody UserUpdateDto dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponseDto> createUser(@RequestBody UserCreateDto registrationDto) {
        return ResponseEntity.ok(userService.adminCreateUser(registrationDto));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserResponseDto> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String role = body.get("role");
        return ResponseEntity.ok(userService.updateUserRole(id, role));
    }

    @PatchMapping("/users/{id}/toggle-enabled")
    public ResponseEntity<UserResponseDto> toggleUserEnabled(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserEnabled(id));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}/conversions")
    public ResponseEntity<List<ConversionResponseDto>> getUserConversions(@PathVariable Long id) {
        return ResponseEntity.ok(conversionService.findByUserId(id));
    }

    // ===== CONVERSIONS MANAGEMENT =====

    @GetMapping("/conversions")
    public ResponseEntity<List<ConversionResponseDto>> getAllConversions() {
        return ResponseEntity.ok(conversionService.getAllConversions());
    }

    @DeleteMapping("/conversions/{id}")
    public ResponseEntity<Void> deleteConversion(@PathVariable Long id) {
        conversionService.deleteConversionById(id);
        return ResponseEntity.noContent().build();
    }

    // ===== SYSTEM HEALTH =====

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();

        // Database check
        try {
            long userCount = userRepository.count();
            health.put("database", Map.of("status", "UP", "userCount", userCount));
        } catch (Exception e) {
            health.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        // App uptime
        health.put("uptime", System.currentTimeMillis());
        health.put("status", "UP");
        health.put("javaVersion", System.getProperty("java.version"));
        health.put("memory", Map.of(
            "total", Runtime.getRuntime().totalMemory(),
            "free", Runtime.getRuntime().freeMemory(),
            "used", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        ));

        return ResponseEntity.ok(health);
    }
}
