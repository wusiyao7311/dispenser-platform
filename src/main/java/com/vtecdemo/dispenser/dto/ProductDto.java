package com.vtecdemo.dispenser.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        @NotBlank String sku,
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal unitPrice
) {
}
