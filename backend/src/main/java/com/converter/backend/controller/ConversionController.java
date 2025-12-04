package com.converter.backend.controller;

import java.net.URI;
import java.util.List;

import com.converter.backend.dto.conversion.ConversionCreateDto;
import com.converter.backend.dto.conversion.ConversionResponseDto;
import com.converter.backend.model.Conversion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.converter.backend.service.ConversionService;

@RestController
@RequestMapping("/api/conversions")
public class ConversionController {

    private final ConversionService conversionService ; 

    public ConversionController(ConversionService conversionService){
        this.conversionService = conversionService ; 
    }


    @GetMapping
    public ResponseEntity<List<ConversionResponseDto>> getAllConversions(){
        return ResponseEntity.ok(conversionService.getAllConversions());
    }
    @GetMapping("/{id}")
    public ResponseEntity<ConversionResponseDto> getConversionById(@PathVariable Long id){
        return ResponseEntity.ok(conversionService.getConversionById(id));
    }

    @PostMapping
    public ResponseEntity<ConversionResponseDto> createConversion(@RequestBody ConversionCreateDto conversion){

        ConversionResponseDto createdConversion = conversionService.createConversion(conversion);

        return ResponseEntity
            .created(URI.create("/conversion/" + createdConversion.getId()))
            .body(createdConversion);
    }



}
