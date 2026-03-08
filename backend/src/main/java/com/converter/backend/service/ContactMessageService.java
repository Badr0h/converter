package com.converter.backend.service;

import com.converter.backend.dto.contact.ContactMessageCreateDto;
import com.converter.backend.dto.contact.ContactMessageReplyDto;
import com.converter.backend.dto.contact.ContactMessageResponseDto;
import com.converter.backend.model.ContactMessage;
import com.converter.backend.model.User;
import com.converter.backend.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;

    @Transactional
    public ContactMessageResponseDto createMessage(ContactMessageCreateDto dto, User user) {
        ContactMessage message = ContactMessage.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .subject(dto.getSubject())
                .message(dto.getMessage())
                .user(user)
                .isRead(false)
                .isReplied(false)
                .build();
        
        ContactMessage saved = contactMessageRepository.save(message);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<ContactMessageResponseDto> getAllMessages(Pageable pageable) {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public ContactMessageResponseDto getMessageById(Long id) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        if (!message.isRead()) {
            message.setRead(true);
            message = contactMessageRepository.save(message);
        }
        
        return mapToDto(message);
    }

    @Transactional
    public ContactMessageResponseDto replyToMessage(Long id, ContactMessageReplyDto replyDto) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        message.setReplyMessage(replyDto.getReplyMessage());
        message.setRepliedAt(LocalDateTime.now());
        message.setReplied(true);
        message.setRead(true);
        
        ContactMessage saved = contactMessageRepository.save(message);
        
        // TODO: Actually send an email to the user here.
        
        return mapToDto(saved);
    }

    private ContactMessageResponseDto mapToDto(ContactMessage message) {
        return ContactMessageResponseDto.builder()
                .id(message.getId())
                .name(message.getName())
                .email(message.getEmail())
                .subject(message.getSubject())
                .message(message.getMessage())
                .replyMessage(message.getReplyMessage())
                .repliedAt(message.getRepliedAt())
                .isRead(message.isRead())
                .isReplied(message.isReplied())
                .createdAt(message.getCreatedAt())
                .userId(message.getUser() != null ? message.getUser().getId() : null)
                .build();
    }
}
