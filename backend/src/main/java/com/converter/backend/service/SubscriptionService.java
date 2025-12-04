package com.converter.backend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.converter.backend.dto.subscription.SubscriptionCreateDto;
import com.converter.backend.dto.subscription.SubscriptionResponseDto;
import com.converter.backend.exception.IllegalStateException;
import com.converter.backend.exception.ResourceNotFoundException;
import com.converter.backend.model.Plan;
import com.converter.backend.model.Subscription;
import com.converter.backend.model.Subscription.Status;
import com.converter.backend.repository.PlanRepository;
import com.converter.backend.repository.SubscriptionRepository;

@Service
public class SubscriptionService {


    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository ; 
    private final PlanRepository planRepository ; 

    public SubscriptionService(SubscriptionRepository subscriptionRepository,PlanRepository planRepository){
        this.subscriptionRepository = subscriptionRepository ;
        this.planRepository = planRepository ;
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


    @Transactional
    public SubscriptionResponseDto createSubscription(SubscriptionCreateDto dto){

        Plan plan = planRepository.findById(dto.getPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id " + dto.getPlanId()));

        Subscription subscription = new Subscription() ; 
        subscription.setStartDate(dto.getStartDate());
        subscription.setDuration(dto.getDuration());
        subscription.setPlan(plan);
        subscription.setStatus(Status.PENDING);

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
        }else{
        subscription.setStatus(Status.ACTIVE);
        }

        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        return mapToSubscriptionResponseDto(updatedSubscription);

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
        dto.setPlanName(subscription.getPlan().getName());
        dto.setPrice(subscription.getPlan().getPrice());

        return dto ;
    }


}
