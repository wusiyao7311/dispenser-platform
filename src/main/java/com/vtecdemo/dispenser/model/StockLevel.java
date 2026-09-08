package com.vtecdemo.dispenser.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(
    name = "stock_level",
    uniqueConstraints = @UniqueConstraint(columnNames = {"dispenser_id", "product_id"})
)
public class StockLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispenser_id", nullable = false)
    private Dispenser dispenser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer quantity = 0;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "last_restocked_at")
    private Instant lastRestockedAt;

    public StockLevel() {
    }

    public StockLevel(Dispenser dispenser, Product product, Integer quantity, Integer capacity) {
        this.dispenser = dispenser;
        this.product = product;
        this.quantity = quantity;
        this.capacity = capacity;
    }

    public boolean isLowStock(double thresholdRatio) {
        return capacity > 0 && ((double) quantity / capacity) <= thresholdRatio;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dispenser getDispenser() {
        return dispenser;
    }

    public void setDispenser(Dispenser dispenser) {
        this.dispenser = dispenser;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Instant getLastRestockedAt() {
        return lastRestockedAt;
    }

    public void setLastRestockedAt(Instant lastRestockedAt) {
        this.lastRestockedAt = lastRestockedAt;
    }
}
