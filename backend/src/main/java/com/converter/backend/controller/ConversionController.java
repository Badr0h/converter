package com.converter.backend.controller;

import java.net.URI;
import java.util.List;

import com.converter.backend.dto.conversion.ConversionCreateDto;
import com.converter.backend.dto.conversion.ConversionResponseDto;
import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.converter.backend.service.ConversionService;
import com.converter.backend.model.User;
import com.converter.backend.service.AuthService ; 

@RestController
@RequestMapping("/api/conversions")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;
    private final AuthService authService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversionResponseDto>> getUserConversions(){
        User currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(conversionService.findByUserId(currentUser.getId()));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversionResponseDto> getConversionById(@PathVariable Long id){
        User currentUser = authService.getCurrentUser();

        ConversionResponseDto conversion = conversionService.getConversionById(id);
        
        // Vérifier que l'utilisateur est le propriétaire de la conversion
        if (!conversion.getUserId().equals(currentUser.getId())) {
            throw new SecurityException("Access denied: You can only access your own conversions");
        }
        
        return ResponseEntity.ok(conversion);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversionResponseDto> createConversion(@Valid @RequestBody ConversionCreateDto conversion) {
        User currentUser = authService.getCurrentUser();

        ConversionResponseDto createdConversion = conversionService.createConversion(conversion, currentUser.getId());

        return ResponseEntity
            .created(URI.create("/conversions/" + createdConversion.getId()))
            .body(createdConversion);
    }



}
