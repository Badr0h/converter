package com.converter.backend.dto.conversion;

import java.time.LocalDateTime;

import com.converter.backend.model.Conversion.InputFormat;
import com.converter.backend.model.Conversion.OutputFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConversionResponseDto {
    private Long id ; 
    private OutputFormat outputFormat ; 
    private InputFormat inputFormat ; 
    private String aiResponse ; 
    private String prompt ; 
    private LocalDateTime createdAt ;
}
