package com.vtecdemo.dispenser.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dispenser")
public class Dispenser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @NotBlank
    @Column(nullable = false)
    private String location;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DispenserStatus status = DispenserStatus.ACTIVE;

    @Column(name = "last_serviced_at")
    private Instant lastServicedAt;

    @OneToMany(mappedBy = "dispenser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockLevel> stockLevels = new ArrayList<>();

    public Dispenser() {
    }

    public Dispenser(String code, String location, DispenserStatus status) {
        this.code = code;
        this.location = location;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public DispenserStatus getStatus() {
        return status;
    }

    public void setStatus(DispenserStatus status) {
        this.status = status;
    }

    public Instant getLastServicedAt() {
        return lastServicedAt;
    }

    public void setLastServicedAt(Instant lastServicedAt) {
        this.lastServicedAt = lastServicedAt;
    }

    public List<StockLevel> getStockLevels() {
        return stockLevels;
    }

    public void setStockLevels(List<StockLevel> stockLevels) {
        this.stockLevels = stockLevels;
    }
}
