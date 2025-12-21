package com.converter.backend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.converter.backend.dto.dashboard.DashboardStatsDto;
import com.converter.backend.dto.subscription.SubscriptionCreateDto;
import com.converter.backend.dto.subscription.SubscriptionResponseDto;
import com.converter.backend.exception.IllegalStateException;
import com.converter.backend.exception.ResourceNotFoundException;
import com.converter.backend.model.Plan;
import com.converter.backend.model.Subscription;
import com.converter.backend.model.User;
import com.converter.backend.model.Subscription.Status;
import com.converter.backend.repository.PlanRepository;
import com.converter.backend.repository.SubscriptionRepository;
import com.converter.backend.repository.UserRepository;

@Service
public class SubscriptionService {


    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository ; 
    private final PlanRepository planRepository ; 
    private final UserRepository userRepository;
    private final ConversionService conversionService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, PlanRepository planRepository, UserRepository userRepository, ConversionService conversionService){
        this.subscriptionRepository = subscriptionRepository ;
        this.planRepository = planRepository ;
        this.userRepository = userRepository;
        this.conversionService = conversionService;
    }

    public List<SubscriptionResponseDto> getAllSubscriptions(){
        return subscriptionRepository.findAll()
            .stream()
            .map(this::mapToSubscriptionResponseDto)
            .toList();
    }

    public SubscriptionResponseDto getSubscriptionById(Long id){
        Subscription subscription =  subscriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("subscription not found with id : " + id));
        
        return mapToSubscriptionResponseDto(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponseDto getCurrentUserSubscription(){
        // Get the current authenticated user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByEmail(currentUsername)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUsername));

        // Try to find ACTIVE subscription first
        var activeSubscription = subscriptionRepository.findByUserAndStatus(user.getId(), Status.ACTIVE.name());
        if (activeSubscription.isPresent()) {
            return mapToSubscriptionResponseDto(activeSubscription.get());
        }

        // If no active, find PENDING subscription
        var pendingSubscriptions = subscriptionRepository.findByUser(user)
            .stream()
            .filter(s -> s.getStatus() == Status.PENDING)
            .findFirst();
        
        if (pendingSubscriptions.isPresent()) {
            return mapToSubscriptionResponseDto(pendingSubscriptions.get());
        }

        // No active or pending subscription found, throw exception
        throw new ResourceNotFoundException("No active or pending subscription found for user: " + currentUsername);
    }


    @Transactional
    public SubscriptionResponseDto createSubscription(SubscriptionCreateDto dto){

        Plan plan = planRepository.findById(dto.getPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id " + dto.getPlanId()));

        // Get the current authenticated user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByEmail(currentUsername)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUsername));

        // Cancel any existing ACTIVE subscription
        var existingActive = subscriptionRepository.findByUserAndStatus(user.getId(), Status.ACTIVE.name());
        if (existingActive.isPresent()) {
            existingActive.get().setStatus(Status.CANCELLED);
            subscriptionRepository.save(existingActive.get());
        }

        Subscription subscription = new Subscription() ; 
        subscription.setUser(user);
        subscription.setStartDate(dto.getStartDate());
        subscription.setDuration(dto.getDuration());
        subscription.setPlan(plan);
        subscription.setStatus(Status.PENDING);
        subscription.setMaxConversionsPerMonth(plan.getMaxConversions());

        LocalDate endDate = subscription.getStartDate().plusMonths(subscription.getDuration().getMonths());
        subscription.setEndDate(endDate);
        
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        return mapToSubscriptionResponseDto(savedSubscription);
    }

    @Transactional
    public SubscriptionResponseDto  activateSubscription(Long id){
        Subscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("subscription not found with id : " + id ));
        
        if(subscription.getStatus() == Status.ACTIVE){
            throw new IllegalStateException("subscription is still  active until :" + subscription.getEndDate());
        }

        // Cancel any other ACTIVE subscriptions for this user
        var otherActive = subscriptionRepository.findByUserAndStatus(subscription.getUser().getId(), Status.ACTIVE.name());
        if (otherActive.isPresent() && !otherActive.get().getId().equals(id)) {
            otherActive.get().setStatus(Status.CANCELLED);
            subscriptionRepository.save(otherActive.get());
        }

        subscription.setStatus(Status.ACTIVE);
        
        // Ensure maxConversionsPerMonth is set from plan
        if (subscription.getPlan() != null && subscription.getMaxConversionsPerMonth() == null) {
            subscription.setMaxConversionsPerMonth(subscription.getPlan().getMaxConversions());
        }

        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        return mapToSubscriptionResponseDto(updatedSubscription);
    }

    @Transactional
    public SubscriptionResponseDto getOrCreateFreeSubscriptionForUser(User user) {
        // Check if user already has a subscription
        List<Subscription> userSubscriptions = subscriptionRepository.findByUser(user);
        if (!userSubscriptions.isEmpty()) {
            return mapToSubscriptionResponseDto(userSubscriptions.get(0));
        }

        // Create a free tier subscription
        Subscription freeSubscription = new Subscription();
        freeSubscription.setUser(user);
        freeSubscription.setStartDate(LocalDate.now());
        freeSubscription.setEndDate(LocalDate.now().plusMonths(1)); // Set to expire in one month
        freeSubscription.setDuration(Subscription.SubscriptionDuration.ONE_MONTH);
        freeSubscription.setPlan(null); // No plan for free tier
        freeSubscription.setStatus(Status.ACTIVE);
        freeSubscription.setMaxConversionsPerMonth(1); // 1 free conversion per month

        Subscription savedSubscription = subscriptionRepository.save(freeSubscription);
        return mapToSubscriptionResponseDto(savedSubscription);
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        try {
            // Get current user's subscription
            SubscriptionResponseDto subscription = getCurrentUserSubscription();
            
            // Get current month's conversion count
            long currentMonthConversions = conversionService.getCurrentUserMonthlyConversionCount();
            
            // Calculate remaining conversions
            Integer maxConversions = subscription.getMaxConversionsPerMonth();
            long remainingConversions = maxConversions != null ? 
                Math.max(0, maxConversions - currentMonthConversions) : 0;
            
            DashboardStatsDto stats = new DashboardStatsDto();
            stats.setTotalConversions(currentMonthConversions);
            stats.setRemainingConversions(remainingConversions);
            stats.setSubscriptionStatus(subscription.getStatus().toString());
            stats.setMaxConversionsPerMonth(maxConversions);
            
            return stats;
        } catch (ResourceNotFoundException e) {
            // User has no subscription, return free tier defaults
            DashboardStatsDto stats = new DashboardStatsDto();
            stats.setTotalConversions(0L);
            stats.setRemainingConversions(1L); // 1 free conversion
            stats.setSubscriptionStatus("FREE");
            stats.setMaxConversionsPerMonth(1);
            return stats;
        }
    }

    @Transactional
    public void expireOldSunscriptions(){
        List<Subscription> activeSubscriptions = subscriptionRepository
            .findByEndDateBeforeAndStatus(LocalDate.now(), Subscription.Status.ACTIVE);
        activeSubscriptions.forEach(sub -> sub.setStatus(Subscription.Status.EXPIRED));

        if (!activeSubscriptions.isEmpty()) {
            subscriptionRepository.saveAll(activeSubscriptions);
            logger.info(" Expired {} subscriptions.", activeSubscriptions.size());
        }else {
            logger.debug("No subscriptions to expire today.");
        }
    }

    
    private SubscriptionResponseDto mapToSubscriptionResponseDto(Subscription subscription){

        SubscriptionResponseDto dto = new SubscriptionResponseDto();
        dto.setId(subscription.getId());
        dto.setStatus(subscription.getStatus());
        dto.setDuration(subscription.getDuration());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());
        dto.setCreatedAt(subscription.getCreatedAt());
        
        // Handle null plan for free tier
        if (subscription.getPlan() != null) {
            dto.setPlanName(subscription.getPlan().getName());
            dto.setPlanId(subscription.getPlan().getId());
            dto.setPrice(subscription.getPlan().getPrice());
        } else {
            dto.setPlanName("Free");
            dto.setPlanId(null);
            dto.setPrice(java.math.BigDecimal.ZERO);
        }
        
        dto.setMaxConversionsPerMonth(subscription.getMaxConversionsPerMonth());

        return dto ;
    }


}
