package com.vtecdemo.dispenser.service;

import com.vtecdemo.dispenser.dto.ImportResult;
import com.vtecdemo.dispenser.model.Dispenser;
import com.vtecdemo.dispenser.model.DispenserStatus;
import com.vtecdemo.dispenser.repository.DispenserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV import/export for dispensers, standing in for the "import and export
 * interfaces" work item in the job description. A real integration would
 * likely exchange XML/JSON with an upstream ERP or fleet-management system;
 * CSV keeps this demo runnable without an external dependency.
 */
@Service
@Transactional
public class DispenserImportExportService {

    private static final String CSV_HEADER = "code,location,status";

    private final DispenserRepository dispenserRepository;

    public DispenserImportExportService(DispenserRepository dispenserRepository) {
        this.dispenserRepository = dispenserRepository;
    }

    public ImportResult importCsv(InputStream inputStream) {
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length < 3) {
                    errors.add("Line " + lineNumber + ": expected 3 columns (code,location,status), got " + parts.length);
                    skipped++;
                    continue;
                }

                String code = parts[0].trim();
                String location = parts[1].trim();
                String statusRaw = parts[2].trim();

                if (dispenserRepository.existsByCode(code)) {
                    errors.add("Line " + lineNumber + ": dispenser '" + code + "' already exists, skipped");
                    skipped++;
                    continue;
                }

                try {
                    DispenserStatus status = DispenserStatus.valueOf(statusRaw.toUpperCase());
                    dispenserRepository.save(new Dispenser(code, location, status));
                    imported++;
                } catch (IllegalArgumentException e) {
                    errors.add("Line " + lineNumber + ": invalid status '" + statusRaw + "'");
                    skipped++;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read import file", e);
        }

        return new ImportResult(imported, skipped, errors);
    }

    public String exportCsv() {
        Writer writer = new StringWriter();
        try {
            writer.write(CSV_HEADER);
            writer.write(System.lineSeparator());
            for (Dispenser d : dispenserRepository.findAll()) {
                writer.write(String.join(",", d.getCode(), d.getLocation(), d.getStatus().name()));
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write export", e);
        }
        return writer.toString();
    }
}
