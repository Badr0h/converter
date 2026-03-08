package com.converter.backend.dto.contact;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ContactMessageResponseDto {
    private Long id;
    private String name;
    private String email;
    private String subject;
    private String message;
    private String replyMessage;
    private LocalDateTime repliedAt;
    private boolean isRead;
    private boolean isReplied;
    private LocalDateTime createdAt;
    private Long userId;
}
