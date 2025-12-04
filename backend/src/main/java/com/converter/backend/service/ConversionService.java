package com.converter.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import com.converter.backend.dto.conversion.ConversionCreateDto;
import com.converter.backend.dto.conversion.ConversionResponseDto;
import com.converter.backend.exception.ResourceNotFoundException;
import com.converter.backend.model.Conversion;
import com.converter.backend.repository.ConversionRepository;
import java.util.List;

@Service
public class ConversionService {


    private final ConversionRepository conversionRepository ; 

    private final AiService aiService ; 




    public ConversionService(ConversionRepository conversionRepository, AiService aiService){
        this.conversionRepository = conversionRepository ; 
        this.aiService = aiService ; 
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

        return aiService.generateResponse(conversion.getPrompt())
                .flatMap(aiResponse -> {
                    conversion.setAiResponse(aiResponse);
                    Conversion savedConversion = conversionRepository.save(conversion);
                    return Mono.just(mapToConversionResponseDto(savedConversion));
                }).block(); // Bloque ici pour retourner un objet simple au contrôleur
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
