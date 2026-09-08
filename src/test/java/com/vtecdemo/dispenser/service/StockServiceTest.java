package com.vtecdemo.dispenser.service;

import com.vtecdemo.dispenser.dto.RestockRequest;
import com.vtecdemo.dispenser.dto.StockLevelDto;
import com.vtecdemo.dispenser.model.Dispenser;
import com.vtecdemo.dispenser.model.DispenserStatus;
import com.vtecdemo.dispenser.model.Product;
import com.vtecdemo.dispenser.model.StockLevel;
import com.vtecdemo.dispenser.repository.DispenserRepository;
import com.vtecdemo.dispenser.repository.ProductRepository;
import com.vtecdemo.dispenser.repository.StockLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockLevelRepository stockLevelRepository;
    @Mock
    private DispenserRepository dispenserRepository;
    @Mock
    private ProductRepository productRepository;

    private StockService stockService;

    private Dispenser dispenser;
    private Product product;

    @BeforeEach
    void setUp() {
        stockService = new StockService(stockLevelRepository, dispenserRepository, productRepository);
        dispenser = new Dispenser("DSP-01", "Test Site", DispenserStatus.ACTIVE);
        product = new Product("SKU-1", "Water", new BigDecimal("1.20"));
    }

    @Test
    void restock_existingStockLevel_incrementsQuantityWithoutExceedingCapacity() {
        StockLevel existing = new StockLevel(dispenser, product, 45, 50);
        RestockRequest request = new RestockRequest(1L, 10, null);

        when(dispenserRepository.findById(1L)).thenReturn(Optional.of(dispenser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockLevelRepository.findByDispenserIdAndProductId(1L, 1L)).thenReturn(Optional.of(existing));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        StockLevelDto result = stockService.restock(1L, request);

        assertThat(result.quantity()).isEqualTo(50); // capped at capacity, not 55
        assertThat(result.capacity()).isEqualTo(50);
    }

    @Test
    void restock_noExistingStockLevel_createsOneUsingRequestedCapacity() {
        RestockRequest request = new RestockRequest(1L, 20, 40);

        when(dispenserRepository.findById(1L)).thenReturn(Optional.of(dispenser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockLevelRepository.findByDispenserIdAndProductId(1L, 1L)).thenReturn(Optional.empty());

        ArgumentCaptor<StockLevel> captor = ArgumentCaptor.forClass(StockLevel.class);
        when(stockLevelRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        StockLevelDto result = stockService.restock(1L, request);

        assertThat(captor.getValue().getCapacity()).isEqualTo(40);
        assertThat(result.quantity()).isEqualTo(20);
    }

    @Test
    void restock_unknownDispenser_throwsNotFound() {
        when(dispenserRepository.findById(99L)).thenReturn(Optional.empty());
        RestockRequest request = new RestockRequest(1L, 10, null);

        assertThatThrownBy(() -> stockService.restock(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(productRepository);
        verify(stockLevelRepository, never()).save(any());
    }
}
