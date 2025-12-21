package com.converter.backend.dto.conversion;

import java.time.LocalDateTime;

import com.converter.backend.model.Conversion.Format;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConversionResponseDto {
    private Long id ; 
    private Format outputFormat ; 
    private Format inputFormat ; 
    private String aiResponse ; 
    private String prompt ; 
    private LocalDateTime createdAt ;
}
