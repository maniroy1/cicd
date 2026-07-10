package com.example.inventory.service;

import com.example.inventory.exception.DuplicateSkuException;
import com.example.inventory.exception.ItemNotFoundException;
import com.example.inventory.model.InventoryItem;
import com.example.inventory.repository.InventoryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository repository;

    @InjectMocks
    private InventoryService service;

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        item = new InventoryItem("Laptop", "SKU-001", "Dell XPS 15", 25,
                new BigDecimal("1299.99"), "Electronics");
        item.setId(1L);
    }

    @Test
    void getAllItems_returnsAllItems() {
        when(repository.findAll()).thenReturn(List.of(item));

        List<InventoryItem> result = service.getAllItems();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    void getItemById_whenExists_returnsItem() {
        when(repository.findById(1L)).thenReturn(Optional.of(item));

        InventoryItem result = service.getItemById(1L);

        assertThat(result.getSku()).isEqualTo("SKU-001");
    }

    @Test
    void getItemById_whenNotFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getItemById(99L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getItemBySku_whenExists_returnsItem() {
        when(repository.findBySku("SKU-001")).thenReturn(Optional.of(item));

        InventoryItem result = service.getItemBySku("SKU-001");

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void createItem_withUniqueSku_savesItem() {
        when(repository.existsBySku("SKU-001")).thenReturn(false);
        when(repository.save(any(InventoryItem.class))).thenReturn(item);

        InventoryItem result = service.createItem(item);

        assertThat(result.getName()).isEqualTo("Laptop");
        verify(repository).save(item);
    }

    @Test
    void createItem_withDuplicateSku_throwsException() {
        when(repository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> service.createItem(item))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("SKU-001");
        verify(repository, never()).save(any());
    }

    @Test
    void updateItem_whenExists_updatesFields() {
        InventoryItem updated = new InventoryItem("Laptop Pro", "SKU-001", "Updated", 30,
                new BigDecimal("1499.99"), "Electronics");
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        when(repository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryItem result = service.updateItem(1L, updated);

        assertThat(result.getName()).isEqualTo("Laptop Pro");
        assertThat(result.getQuantity()).isEqualTo(30);
        assertThat(result.getPrice()).isEqualByComparingTo("1499.99");
    }

    @Test
    void updateItem_whenNotFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateItem(99L, item))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void adjustQuantity_increasesStock() {
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        when(repository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryItem result = service.adjustQuantity(1L, 10);

        assertThat(result.getQuantity()).isEqualTo(35);
    }

    @Test
    void adjustQuantity_belowZero_throwsException() {
        when(repository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.adjustQuantity(1L, -100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void deleteItem_whenExists_deletesItem() {
        when(repository.existsById(1L)).thenReturn(true);

        service.deleteItem(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteItem_whenNotFound_throwsException() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteItem(99L))
                .isInstanceOf(ItemNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}
