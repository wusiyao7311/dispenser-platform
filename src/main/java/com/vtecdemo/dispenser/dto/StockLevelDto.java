package com.vtecdemo.dispenser.dto;

import java.time.Instant;

public record StockLevelDto(
        Long id,
        Long dispenserId,
        String dispenserCode,
        Long productId,
        String productSku,
        String productName,
        Integer quantity,
        Integer capacity,
        Instant lastRestockedAt
) {
}
