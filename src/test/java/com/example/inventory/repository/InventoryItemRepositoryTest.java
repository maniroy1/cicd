package com.example.inventory.repository;

import com.example.inventory.model.InventoryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class InventoryItemRepositoryTest {

    @Autowired
    private InventoryItemRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(new InventoryItem("Laptop", "SKU-001", "Dell XPS", 25,
                new BigDecimal("1299.99"), "Electronics"));
        repository.save(new InventoryItem("Mouse", "SKU-002", "Wireless mouse", 5,
                new BigDecimal("29.99"), "Electronics"));
        repository.save(new InventoryItem("Desk", "SKU-003", "Standing desk", 12,
                new BigDecimal("499.00"), "Furniture"));
    }

    @Test
    void findBySku_returnsMatchingItem() {
        Optional<InventoryItem> result = repository.findBySku("SKU-002");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Mouse");
    }

    @Test
    void findBySku_whenMissing_returnsEmpty() {
        assertThat(repository.findBySku("NOPE")).isEmpty();
    }

    @Test
    void findByCategoryIgnoreCase_matchesCaseInsensitively() {
        List<InventoryItem> result = repository.findByCategoryIgnoreCase("electronics");

        assertThat(result).hasSize(2);
    }

    @Test
    void findByQuantityLessThan_returnsLowStockItems() {
        List<InventoryItem> result = repository.findByQuantityLessThan(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("SKU-002");
    }

    @Test
    void existsBySku_returnsTrueForExistingSku() {
        assertThat(repository.existsBySku("SKU-001")).isTrue();
        assertThat(repository.existsBySku("SKU-999")).isFalse();
    }

    @Test
    void save_setsCreatedAndUpdatedTimestamps() {
        InventoryItem saved = repository.saveAndFlush(new InventoryItem("Chair", "SKU-004",
                "Office chair", 8, new BigDecimal("199.99"), "Furniture"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
