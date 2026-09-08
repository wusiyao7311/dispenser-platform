package com.vtecdemo.dispenser.controller;

import com.vtecdemo.dispenser.dto.DispenserDto;
import com.vtecdemo.dispenser.dto.RestockRequest;
import com.vtecdemo.dispenser.dto.StockLevelDto;
import com.vtecdemo.dispenser.service.DispenserService;
import com.vtecdemo.dispenser.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispensers")
public class DispenserController {

    private final DispenserService dispenserService;
    private final StockService stockService;

    public DispenserController(DispenserService dispenserService, StockService stockService) {
        this.dispenserService = dispenserService;
        this.stockService = stockService;
    }

    @GetMapping
    public List<DispenserDto> findAll() {
        return dispenserService.findAll();
    }

    @GetMapping("/{id}")
    public DispenserDto findById(@PathVariable Long id) {
        return dispenserService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DispenserDto create(@Valid @RequestBody DispenserDto dto) {
        return dispenserService.create(dto);
    }

    @PutMapping("/{id}")
    public DispenserDto update(@PathVariable Long id, @Valid @RequestBody DispenserDto dto) {
        return dispenserService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        dispenserService.delete(id);
    }

    @GetMapping("/{id}/stock")
    public List<StockLevelDto> stock(@PathVariable Long id) {
        return stockService.findByDispenser(id);
    }

    @PostMapping("/{id}/restock")
    public StockLevelDto restock(@PathVariable Long id, @Valid @RequestBody RestockRequest request) {
        return stockService.restock(id, request);
    }

    @GetMapping("/low-stock")
    public List<StockLevelDto> lowStock() {
        return stockService.findLowStock();
    }
}
