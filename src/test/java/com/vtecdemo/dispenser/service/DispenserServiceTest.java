package com.vtecdemo.dispenser.service;

import com.vtecdemo.dispenser.dto.DispenserDto;
import com.vtecdemo.dispenser.model.Dispenser;
import com.vtecdemo.dispenser.model.DispenserStatus;
import com.vtecdemo.dispenser.repository.DispenserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispenserServiceTest {

    @Mock
    private DispenserRepository dispenserRepository;

    private DispenserService dispenserService;

    @BeforeEach
    void setUp() {
        dispenserService = new DispenserService(dispenserRepository);
    }

    @Test
    void create_duplicateCode_throwsConflict() {
        DispenserDto dto = new DispenserDto(null, "DSP-01", "Site A", DispenserStatus.ACTIVE, null);
        when(dispenserRepository.existsByCode("DSP-01")).thenReturn(true);

        assertThatThrownBy(() -> dispenserService.create(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("DSP-01");
    }

    @Test
    void create_newCode_savesAndReturnsDto() {
        DispenserDto dto = new DispenserDto(null, "DSP-02", "Site B", DispenserStatus.ACTIVE, null);
        when(dispenserRepository.existsByCode("DSP-02")).thenReturn(false);
        when(dispenserRepository.save(any(Dispenser.class))).thenAnswer(inv -> {
            Dispenser d = inv.getArgument(0);
            d.setId(7L);
            return d;
        });

        DispenserDto result = dispenserService.create(dto);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.code()).isEqualTo("DSP-02");
    }

    @Test
    void findById_unknownId_throwsNotFound() {
        when(dispenserRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dispenserService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
