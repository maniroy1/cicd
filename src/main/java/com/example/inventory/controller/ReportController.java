package com.example.inventory.controller;

import com.example.inventory.model.InventoryItem;
import com.example.inventory.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Reporting endpoints for inventory data. All data access and privileged
 * operations are delegated to {@link ReportService}.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Api-Key";
    private static final int MAX_PAGE_SIZE = 100;

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Paged search of inventory items by name. */
    @GetMapping("/search")
    public Page<InventoryItem> search(@RequestParam String name,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return reportService.searchByName(name, pageable);
    }

    /** Export a previously generated report file. Requires an admin API key. */
    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam String file,
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey) {

        if (!reportService.isAuthorizedAdmin(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<String> content = reportService.readReport(file);
        return content.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Average price across all inventory items. */
    @GetMapping("/average-price")
    public BigDecimal averagePrice() {
        return reportService.averagePrice();
    }

    /** Value summary for a single item. */
    @GetMapping("/{id}/summary")
    public Map<String, Object> summary(@PathVariable Long id) {
        return reportService.summary(id);
    }

    /** Starts a restore from the nightly backup. Requires an admin API key. */
    @PostMapping("/restore")
    public ResponseEntity<String> restore(
            @RequestParam String backupHost,
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey) {

        if (!reportService.isAuthorizedAdmin(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        reportService.startRestore(backupHost);
        return ResponseEntity.accepted().body("restore started");
    }
}
