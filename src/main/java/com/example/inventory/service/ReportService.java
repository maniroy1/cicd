package com.example.inventory.service;

import com.example.inventory.exception.ItemNotFoundException;
import com.example.inventory.model.InventoryItem;
import com.example.inventory.repository.InventoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for the reporting endpoints. Keeps data access and the
 * security-sensitive operations (file reads, restore) out of the web layer.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final InventoryItemRepository repository;
    private final Path reportsDirectory;
    private final String adminApiKey;
    private final Set<String> allowedBackupHosts;
    private final String backupPassword;

    public ReportService(InventoryItemRepository repository,
                         @Value("${reports.directory:/var/reports}") String reportsDirectory,
                         @Value("${admin.api-key:}") String adminApiKey,
                         @Value("${backup.allowed-hosts:}") String allowedBackupHosts,
                         @Value("${backup.password:}") String backupPassword) {
        this.repository = repository;
        this.reportsDirectory = Paths.get(reportsDirectory).toAbsolutePath().normalize();
        this.adminApiKey = adminApiKey;
        this.allowedBackupHosts = Arrays.stream(allowedBackupHosts.split(","))
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.backupPassword = backupPassword;
    }

    /** Paged, parameterized name search - no string concatenation into SQL. */
    @Transactional(readOnly = true)
    public Page<InventoryItem> searchByName(String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Search term must not be blank");
        }
        return repository.findByNameContainingIgnoreCase(name, pageable);
    }

    /** Average price computed by the database, not by loading every row. */
    @Transactional(readOnly = true)
    public BigDecimal averagePrice() {
        Double average = repository.findAveragePrice();
        if (average == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(Long id) {
        InventoryItem item = repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", item.getId());
        summary.put("sku", item.getSku());
        summary.put("quantity", item.getQuantity());
        summary.put("totalValue", item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return summary;
    }

    /**
     * Reads a report file, refusing any path that resolves outside the configured
     * report directory. Returns empty when the report does not exist.
     */
    public Optional<String> readReport(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Report file name must not be blank");
        }

        Path target = reportsDirectory.resolve(fileName).normalize();
        if (!target.startsWith(reportsDirectory)) {
            log.warn("Rejected report request resolving outside the report directory");
            throw new IllegalArgumentException("Invalid report file name");
        }

        try {
            return Optional.of(Files.readString(target, StandardCharsets.UTF_8));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read report", e);
        }
    }

    /** Constant-time comparison; fails closed when no key is configured. */
    public boolean isAuthorizedAdmin(String presentedKey) {
        if (adminApiKey == null || adminApiKey.isBlank() || presentedKey == null) {
            return false;
        }
        return MessageDigest.isEqual(
                presentedKey.getBytes(StandardCharsets.UTF_8),
                adminApiKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Starts a restore against an allow-listed host. Arguments are passed as a list
     * (no shell, no interpolation) and the password goes via the environment so it
     * never appears in the process arguments.
     */
    public void startRestore(String backupHost) {
        if (backupHost == null || !allowedBackupHosts.contains(backupHost)) {
            log.warn("Rejected restore request for a host that is not allow-listed");
            throw new IllegalArgumentException("Backup host is not allow-listed");
        }

        ProcessBuilder builder = new ProcessBuilder("pg_restore", "-h", backupHost);
        builder.environment().put("PGPASSWORD", backupPassword);
        try {
            builder.start();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start the restore process", e);
        }
        log.info("Restore started for host={}", backupHost);
    }
}
