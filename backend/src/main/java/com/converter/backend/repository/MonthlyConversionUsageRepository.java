package com.converter.backend.repository;

import com.converter.backend.model.MonthlyConversionUsage;
import com.converter.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyConversionUsageRepository extends JpaRepository<MonthlyConversionUsage, Long> {
    
    /**
     * Find usage record for a specific user and year-month
     * 
     * @param user The user
     * @param yearMonth Format: YYYY-MM (e.g., "2026-03")
     * @return Optional containing the usage record
     */
    Optional<MonthlyConversionUsage> findByUserAndYearMonth(User user, String yearMonth);
}
