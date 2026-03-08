package com.converter.backend.controller;

import com.converter.backend.dto.contact.ContactMessageCreateDto;
import com.converter.backend.dto.contact.ContactMessageResponseDto;
import com.converter.backend.model.User;
import com.converter.backend.service.ContactMessageService;
import com.converter.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactMessageService contactMessageService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ContactMessageResponseDto> submitMessage(
            @Valid @RequestBody ContactMessageCreateDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userService.getUserEntityByEmail(userDetails.getUsername());
        return ResponseEntity.ok(contactMessageService.createMessage(dto, user));
    }
}
