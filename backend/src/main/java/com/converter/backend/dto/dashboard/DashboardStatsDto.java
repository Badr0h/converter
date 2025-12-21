package com.converter.backend.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private Long totalConversions;
    private Long remainingConversions;
    private String subscriptionStatus;
    private Integer maxConversionsPerMonth;
}
