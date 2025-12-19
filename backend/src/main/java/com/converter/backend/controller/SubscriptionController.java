package com.converter.backend.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.converter.backend.dto.subscription.SubscriptionResponseDto;
import com.converter.backend.service.SubscriptionService;
import com.converter.backend.dto.subscription.SubscriptionCreateDto;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {


    private final SubscriptionService subscriptionService ; 

    public SubscriptionController(SubscriptionService subscriptionService){
        this.subscriptionService = subscriptionService ;
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponseDto>> getAllSubscriptions(){
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponseDto> getCurrentUserSubscription(){
        return ResponseEntity.ok(subscriptionService.getCurrentUserSubscription());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponseDto> getSubscriptionById(@PathVariable Long id){
        return ResponseEntity.ok(subscriptionService.getSubscriptionById(id));
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponseDto> createSubscription(@Valid @RequestBody SubscriptionCreateDto subscription){
        SubscriptionResponseDto createdSubscription = subscriptionService.createSubscription(subscription);
        return ResponseEntity
            .created(URI.create("/subscription/" + createdSubscription.getId()))
            .body(createdSubscription);
    }

    // Allow both PUT and POST for activation to be resilient to different clients
    @PutMapping("/{id}/activate")
    @PostMapping("/{id}/activate")
    public ResponseEntity<SubscriptionResponseDto> activateSubscription(@PathVariable Long id){
        SubscriptionResponseDto activated = subscriptionService.activateSubscription(id);
        return ResponseEntity.ok(activated);
    }

}