package com.vtecdemo.dispenser.dto;

import com.vtecdemo.dispenser.model.DispenserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record DispenserDto(
        Long id,
        @NotBlank String code,
        @NotBlank String location,
        @NotNull DispenserStatus status,
        Instant lastServicedAt
) {
}
