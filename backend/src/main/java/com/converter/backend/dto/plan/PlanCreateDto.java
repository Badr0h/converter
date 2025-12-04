package com.converter.backend.dto.plan;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PlanCreateDto {

    @NotBlank(message = "Plan name cannot be blank")
    private String name;

    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotBlank(message = "Currency cannot be blank")
    private String currency;

    @NotNull(message = "Duration cannot be null")
    @Positive(message = "Duration must be a positive integer representing days")
    private Integer duration;
}
