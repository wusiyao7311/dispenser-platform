package com.vtecdemo.dispenser.service;

import com.vtecdemo.dispenser.dto.RestockRequest;
import com.vtecdemo.dispenser.dto.StockLevelDto;
import com.vtecdemo.dispenser.model.Dispenser;
import com.vtecdemo.dispenser.model.Product;
import com.vtecdemo.dispenser.model.StockLevel;
import com.vtecdemo.dispenser.repository.DispenserRepository;
import com.vtecdemo.dispenser.repository.ProductRepository;
import com.vtecdemo.dispenser.repository.StockLevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class StockService {

    /** Dispensers at or below 20% capacity are flagged as low stock. */
    private static final double LOW_STOCK_THRESHOLD_RATIO = 0.20;

    private final StockLevelRepository stockLevelRepository;
    private final DispenserRepository dispenserRepository;
    private final ProductRepository productRepository;

    public StockService(StockLevelRepository stockLevelRepository,
                         DispenserRepository dispenserRepository,
                         ProductRepository productRepository) {
        this.stockLevelRepository = stockLevelRepository;
        this.dispenserRepository = dispenserRepository;
        this.productRepository = productRepository;
    }

    public List<StockLevelDto> findByDispenser(Long dispenserId) {
        if (!dispenserRepository.existsById(dispenserId)) {
            throw new ResourceNotFoundException("Dispenser " + dispenserId + " not found");
        }
        return stockLevelRepository.findByDispenserId(dispenserId).stream().map(this::toDto).toList();
    }

    public List<StockLevelDto> findLowStock() {
        return stockLevelRepository.findLowStock(LOW_STOCK_THRESHOLD_RATIO).stream().map(this::toDto).toList();
    }

    public StockLevelDto restock(Long dispenserId, RestockRequest request) {
        Dispenser dispenser = dispenserRepository.findById(dispenserId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispenser " + dispenserId + " not found"));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product " + request.productId() + " not found"));

        StockLevel stockLevel = stockLevelRepository
                .findByDispenserIdAndProductId(dispenserId, request.productId())
                .orElseGet(() -> new StockLevel(dispenser, product, 0,
                        request.capacity() != null ? request.capacity() : request.quantity()));

        int capacity = request.capacity() != null ? request.capacity() : stockLevel.getCapacity();
        int newQuantity = Math.min(capacity, stockLevel.getQuantity() + request.quantity());

        stockLevel.setCapacity(capacity);
        stockLevel.setQuantity(newQuantity);
        stockLevel.setLastRestockedAt(Instant.now());

        return toDto(stockLevelRepository.save(stockLevel));
    }

    private StockLevelDto toDto(StockLevel s) {
        return new StockLevelDto(
                s.getId(),
                s.getDispenser().getId(),
                s.getDispenser().getCode(),
                s.getProduct().getId(),
                s.getProduct().getSku(),
                s.getProduct().getName(),
                s.getQuantity(),
                s.getCapacity(),
                s.getLastRestockedAt()
        );
    }
}
