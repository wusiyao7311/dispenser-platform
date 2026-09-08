package com.vtecdemo.dispenser.repository;

import com.vtecdemo.dispenser.model.Dispenser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DispenserRepository extends JpaRepository<Dispenser, Long> {
    Optional<Dispenser> findByCode(String code);
    boolean existsByCode(String code);
}
