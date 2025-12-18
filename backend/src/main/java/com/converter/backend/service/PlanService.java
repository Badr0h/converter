package com.converter.backend.service;

import com.converter.backend.dto.plan.PlanCreateDto;
import com.converter.backend.dto.plan.PlanResponseDto;
import com.converter.backend.exception.ResourceNotFoundException;
import com.converter.backend.model.Plan;
import com.converter.backend.repository.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanResponseDto> getAllPlans() {
        return planRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanResponseDto getPlanById(Long id) {
        return planRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
    }

    @Transactional
    public PlanResponseDto createPlan(PlanCreateDto dto) {
        Plan plan = new Plan();
        plan.setName(dto.getName());
        // Prefer monthlyPrice if provided, otherwise fallback to legacy price
        if (dto.getMonthlyPrice() != null) {
            plan.setMonthlyPrice(dto.getMonthlyPrice());
            plan.setPrice(dto.getMonthlyPrice());
        } else {
            plan.setPrice(dto.getPrice());
        }
        plan.setCurrency(dto.getCurrency());
        plan.setDuration(dto.getDuration());

        if (dto.getAnnualPrice() != null) {
            plan.setAnnualPrice(dto.getAnnualPrice());
        }

        Plan savedPlan = planRepository.save(plan);
        return mapToDto(savedPlan);
    }

    @Transactional
    public void deletePlan(Long id) {
        if (!planRepository.existsById(id)) {
            throw new ResourceNotFoundException("Plan not found with id: " + id);
        }
        planRepository.deleteById(id);
    }

    private PlanResponseDto mapToDto(Plan plan) {
        PlanResponseDto dto = new PlanResponseDto();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setPrice(plan.getPrice());
        dto.setCurrency(plan.getCurrency());
        dto.setDuration(plan.getDuration());
        dto.setMonthlyPrice(plan.getMonthlyPrice());
        dto.setAnnualPrice(plan.getAnnualPrice());
        return dto;
    }
}

