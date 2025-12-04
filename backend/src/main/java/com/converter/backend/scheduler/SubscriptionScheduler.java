package com.converter.backend.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.converter.backend.service.SubscriptionService;

@Component
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService ; 

    public SubscriptionScheduler(SubscriptionService subscriptionService){
        this.subscriptionService = subscriptionService ; 
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void checkAndExpireSubscription(){
        subscriptionService.expireOldSunscriptions();
    }


}
