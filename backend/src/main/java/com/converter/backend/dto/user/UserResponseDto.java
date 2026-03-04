package com.converter.backend.dto.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.converter.backend.model.User.Role;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor

public class UserResponseDto {
    private Long id;
    private String email;
    private String fullName;
    private Role role;
    private Boolean enabled;
    private LocalDateTime createdAt ;
    private LocalDateTime updatedAt ;

}
