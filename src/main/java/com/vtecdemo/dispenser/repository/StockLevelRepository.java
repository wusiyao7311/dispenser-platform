package com.vtecdemo.dispenser.repository;

import com.vtecdemo.dispenser.model.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    List<StockLevel> findByDispenserId(Long dispenserId);

    Optional<StockLevel> findByDispenserIdAndProductId(Long dispenserId, Long productId);

    @Query("""
        select s from StockLevel s
        where s.capacity > 0
          and (cast(s.quantity as double) / s.capacity) <= :thresholdRatio
        """)
    List<StockLevel> findLowStock(@Param("thresholdRatio") double thresholdRatio);
}
