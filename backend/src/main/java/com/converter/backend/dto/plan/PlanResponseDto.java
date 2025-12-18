package com.converter.backend.dto.plan;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PlanResponseDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private String currency;
    private Integer duration; // in days
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
}
