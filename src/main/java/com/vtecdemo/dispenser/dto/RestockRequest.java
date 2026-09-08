package com.vtecdemo.dispenser.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RestockRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        Integer capacity
) {
}
