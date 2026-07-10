package com.example.inventory.service;

import com.example.inventory.exception.DuplicateSkuException;
import com.example.inventory.exception.ItemNotFoundException;
import com.example.inventory.model.InventoryItem;
import com.example.inventory.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InventoryService {

    private final InventoryItemRepository repository;

    public InventoryService(InventoryItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getAllItems() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public InventoryItem getItemById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public InventoryItem getItemBySku(String sku) {
        return repository.findBySku(sku)
                .orElseThrow(() -> new ItemNotFoundException("Inventory item not found with SKU: " + sku));
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getItemsByCategory(String category) {
        return repository.findByCategoryIgnoreCase(category);
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStockItems(Integer threshold) {
        return repository.findByQuantityLessThan(threshold);
    }

    public InventoryItem createItem(InventoryItem item) {
        if (repository.existsBySku(item.getSku())) {
            throw new DuplicateSkuException(item.getSku());
        }
        item.setId(null);
        return repository.save(item);
    }

    public InventoryItem updateItem(Long id, InventoryItem updated) {
        InventoryItem existing = repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        if (!existing.getSku().equals(updated.getSku()) && repository.existsBySku(updated.getSku())) {
            throw new DuplicateSkuException(updated.getSku());
        }

        existing.setName(updated.getName());
        existing.setSku(updated.getSku());
        existing.setDescription(updated.getDescription());
        existing.setQuantity(updated.getQuantity());
        existing.setPrice(updated.getPrice());
        existing.setCategory(updated.getCategory());
        return repository.save(existing);
    }

    public InventoryItem adjustQuantity(Long id, int delta) {
        InventoryItem existing = repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        int newQuantity = existing.getQuantity() + delta;
        if (newQuantity < 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock: current quantity is " + existing.getQuantity());
        }
        existing.setQuantity(newQuantity);
        return repository.save(existing);
    }

    public void deleteItem(Long id) {
        if (!repository.existsById(id)) {
            throw new ItemNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
