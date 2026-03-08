package com.converter.backend.controller.admin;

import com.converter.backend.dto.contact.ContactMessageReplyDto;
import com.converter.backend.dto.contact.ContactMessageResponseDto;
import com.converter.backend.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/contact")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContactController {

    private final ContactMessageService contactMessageService;

    @GetMapping
    public ResponseEntity<Page<ContactMessageResponseDto>> getAllMessages(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(contactMessageService.getAllMessages(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactMessageResponseDto> getMessageById(@PathVariable Long id) {
        return ResponseEntity.ok(contactMessageService.getMessageById(id));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<ContactMessageResponseDto> replyToMessage(
            @PathVariable Long id,
            @Valid @RequestBody ContactMessageReplyDto replyDto) {
        return ResponseEntity.ok(contactMessageService.replyToMessage(id, replyDto));
    }
}
