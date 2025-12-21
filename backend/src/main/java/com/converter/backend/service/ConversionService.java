package com.converter.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Mono;

import com.converter.backend.dto.conversion.ConversionCreateDto;
import com.converter.backend.dto.conversion.ConversionResponseDto;
import com.converter.backend.exception.ResourceNotFoundException;
import com.converter.backend.model.Conversion;
import com.converter.backend.model.User;
import com.converter.backend.repository.ConversionRepository;
import com.converter.backend.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class ConversionService {


    private final ConversionRepository conversionRepository ; 

    private final AiService aiService ;

    private final UserRepository userRepository;


    public ConversionService(ConversionRepository conversionRepository, AiService aiService, UserRepository userRepository){
        this.conversionRepository = conversionRepository ; 
        this.aiService = aiService ;
        this.userRepository = userRepository;
    }

    public List<ConversionResponseDto> getAllConversions(){
        return conversionRepository.findAll()
                .stream()
                .map(this::mapToConversionResponseDto)
                .toList();  
    }

    public ConversionResponseDto getConversionById(Long id){
        Conversion conversion = conversionRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("conversion not found with id : " + id));

        return mapToConversionResponseDto(conversion);
    }


    @Transactional
    public ConversionResponseDto createConversion(ConversionCreateDto dto){

        Conversion conversion = new Conversion();
        conversion.setInputFormat(dto.getInputFormat());
        conversion.setOutputFormat(dto.getOutputFormat());
        conversion.setPrompt(dto.getPrompt());

        String userPlan = dto.getPlan() != null ? dto.getPlan().toUpperCase() : "BASIC";

        return aiService.generateResponse(conversion.getPrompt(),userPlan)
                .flatMap(aiResponse -> {
                    conversion.setAiResponse(aiResponse);
                    Conversion savedConversion = conversionRepository.save(conversion);
                    return Mono.just(mapToConversionResponseDto(savedConversion));
                }).block(); // Bloque ici pour retourner un objet simple au contrôleur
    }

    @Transactional(readOnly = true)
    public long getCurrentUserMonthlyConversionCount() {
        // Get the current authenticated user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByEmail(currentUsername)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUsername));

        // Get the start of the current month
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        
        return conversionRepository.countByUserIdAndCreatedAtAfter(user.getId(), startOfMonth);
    }

    public ConversionResponseDto mapToConversionResponseDto(Conversion conversion){
        ConversionResponseDto dto = new ConversionResponseDto();
        dto.setId(conversion.getId());
        dto.setInputFormat(conversion.getInputFormat());
        dto.setOutputFormat(conversion.getOutputFormat());
        dto.setAiResponse(conversion.getAiResponse());
        dto.setPrompt(conversion.getPrompt());
        dto.setCreatedAt(conversion.getCreatedAt());
        return dto;
    }
}
