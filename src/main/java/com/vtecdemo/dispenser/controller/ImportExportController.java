package com.vtecdemo.dispenser.controller;

import com.vtecdemo.dispenser.dto.ImportResult;
import com.vtecdemo.dispenser.service.DispenserImportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ImportExportController {

    private final DispenserImportExportService importExportService;

    public ImportExportController(DispenserImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @PostMapping(value = "/import/dispensers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importDispensers(@RequestParam("file") MultipartFile file) throws IOException {
        return importExportService.importCsv(file.getInputStream());
    }

    @GetMapping("/export/dispensers")
    public ResponseEntity<String> exportDispensers() {
        String csv = importExportService.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dispensers.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
