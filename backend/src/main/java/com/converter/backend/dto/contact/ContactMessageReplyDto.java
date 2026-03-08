package com.converter.backend.dto.contact;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactMessageReplyDto {
    @NotBlank(message = "Reply message is required")
    private String replyMessage;
}
