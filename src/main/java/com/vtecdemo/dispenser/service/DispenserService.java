package com.vtecdemo.dispenser.service;

import com.vtecdemo.dispenser.dto.DispenserDto;
import com.vtecdemo.dispenser.model.Dispenser;
import com.vtecdemo.dispenser.repository.DispenserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DispenserService {

    private final DispenserRepository dispenserRepository;

    public DispenserService(DispenserRepository dispenserRepository) {
        this.dispenserRepository = dispenserRepository;
    }

    public List<DispenserDto> findAll() {
        return dispenserRepository.findAll().stream().map(this::toDto).toList();
    }

    public DispenserDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    public DispenserDto create(DispenserDto dto) {
        if (dispenserRepository.existsByCode(dto.code())) {
            throw new DuplicateResourceException("Dispenser with code '" + dto.code() + "' already exists");
        }
        Dispenser saved = dispenserRepository.save(new Dispenser(dto.code(), dto.location(), dto.status()));
        return toDto(saved);
    }

    public DispenserDto update(Long id, DispenserDto dto) {
        Dispenser dispenser = getOrThrow(id);
        dispenser.setLocation(dto.location());
        dispenser.setStatus(dto.status());
        return toDto(dispenserRepository.save(dispenser));
    }

    public void delete(Long id) {
        if (!dispenserRepository.existsById(id)) {
            throw new ResourceNotFoundException("Dispenser " + id + " not found");
        }
        dispenserRepository.deleteById(id);
    }

    Dispenser getOrThrow(Long id) {
        return dispenserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispenser " + id + " not found"));
    }

    private DispenserDto toDto(Dispenser d) {
        return new DispenserDto(d.getId(), d.getCode(), d.getLocation(), d.getStatus(), d.getLastServicedAt());
    }
}
