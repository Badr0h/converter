package com.converter.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;
import java.util.concurrent.CompletableFuture;

import com.converter.backend.dto.conversion.ConversionCreateDto;
import com.converter.backend.dto.conversion.ConversionResponseDto;
import com.converter.backend.exception.IllegalStateException;
import com.converter.backend.exception.ResourceNotFoundException;
import com.converter.backend.model.Conversion;
import com.converter.backend.model.User;
import com.converter.backend.repository.ConversionRepository;
import com.converter.backend.repository.UserRepository;
import com.converter.backend.model.Subscription;
import com.converter.backend.repository.SubscriptionRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class ConversionService {


    private final ConversionRepository conversionRepository ; 

    private final AiService aiService ;

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;


    public ConversionService(ConversionRepository conversionRepository, AiService aiService, UserRepository userRepository, SubscriptionRepository subscriptionRepository){
        this.conversionRepository = conversionRepository ; 
        this.aiService = aiService ;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public Page<ConversionResponseDto> findByUserIdPaginated(Long userId, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return conversionRepository.findByUserId(userId, pageable)
                .map(this::mapToConversionResponseDto);
    }

    public List<ConversionResponseDto> findByUserId(Long userId){
        // Limite à 100 résultats les plus récents pour éviter les surcharges
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"));
        return conversionRepository.findByUserId(userId, pageable)
                .stream()
                .map(this::mapToConversionResponseDto)
                .toList();  
    }

    public List<ConversionResponseDto> getAllConversions(){
        return conversionRepository.findAll()
                .stream()
                .map(this::mapToConversionResponseDto)
                .toList();  
    }

    public List<ConversionResponseDto> getRecentActivity(int limit) {
        org.springframework.data.domain.Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return conversionRepository.findAll(pageable)
                .stream()
                .map(this::mapToConversionResponseDto)
                .toList();
    }

    public void deleteConversionById(Long id) {
        if (!conversionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Conversion not found with id: " + id);
        }
        conversionRepository.deleteById(id);
    }

    public ConversionResponseDto getConversionById(Long id){
        Conversion conversion = conversionRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("conversion not found with id : " + id));

        return mapToConversionResponseDto(conversion);
    }


    @Transactional
    public ConversionResponseDto createConversion(ConversionCreateDto dto, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Enforce paid subscription
        Subscription subscription = subscriptionRepository.findByUserAndStatus(userId, Subscription.Status.ACTIVE.name())
                .orElseThrow(() -> new IllegalStateException("Paid subscription required for conversion. Please upgrade."));

        // Check conversion limits
        long currentMonthConversions = getCurrentUserMonthlyConversionCount();
        if (subscription.getMaxConversionsPerMonth() != null && currentMonthConversions >= subscription.getMaxConversionsPerMonth()) {
            throw new IllegalStateException("Monthly conversion limit reached (" + subscription.getMaxConversionsPerMonth() + "). Please upgrade your plan.");
        }

        Conversion conversion = new Conversion();
        conversion.setInputFormat(dto.getInputFormat());
        conversion.setOutputFormat(dto.getOutputFormat());
        conversion.setPrompt(dto.getPrompt());
        conversion.setUser(user);

        String userPlan = subscription.getPlan() != null ? subscription.getPlan().getName().toUpperCase() : "BASIC";

        try {
            // Synchronous simpler and secure approach
            String aiResponse = aiService.generateResponseSync(conversion.getPrompt(), userPlan);
            conversion.setAiResponse(aiResponse);
            Conversion savedConversion = conversionRepository.save(conversion);
            return mapToConversionResponseDto(savedConversion);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate AI response", e);
        }
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
        dto.setUserId(conversion.getUser() != null ? conversion.getUser().getId() : null);
        dto.setUserEmail(conversion.getUser() != null ? conversion.getUser().getEmail() : null);
        dto.setInputFormat(conversion.getInputFormat());
        dto.setOutputFormat(conversion.getOutputFormat());
        dto.setAiResponse(conversion.getAiResponse());
        dto.setPrompt(conversion.getPrompt());
        dto.setCreatedAt(conversion.getCreatedAt());
        return dto;
    }
}
