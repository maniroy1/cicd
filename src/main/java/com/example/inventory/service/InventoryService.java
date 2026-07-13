package com.example.inventory.service;

import com.example.inventory.exception.DuplicateSkuException;
import com.example.inventory.exception.ItemNotFoundException;
import com.example.inventory.model.InventoryItem;
import com.example.inventory.repository.InventoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository repository;

    public InventoryService(InventoryItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getAllItems() {
        List<InventoryItem> items = repository.findAll();
        log.info("Fetched {} inventory item(s) from the database", items.size());
        return items;
    }

    @Cacheable(value = "items", key = "#id")
    @Transactional(readOnly = true)
    public InventoryItem getItemById(Long id) {
        log.info("Cache miss - loading item id={} from the database", id);
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
            log.warn("Rejected create: SKU '{}' already exists", item.getSku());
            throw new DuplicateSkuException(item.getSku());
        }
        item.setId(null);
        InventoryItem saved = repository.save(item);
        log.info("Created item id={} sku={} name='{}' qty={}",
                saved.getId(), saved.getSku(), saved.getName(), saved.getQuantity());
        return saved;
    }

    @CacheEvict(value = "items", key = "#id")
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
        InventoryItem saved = repository.save(existing);
        log.info("Updated item id={} sku={} qty={}", saved.getId(), saved.getSku(), saved.getQuantity());
        return saved;
    }

    @CacheEvict(value = "items", key = "#id")
    public InventoryItem adjustQuantity(Long id, int delta) {
        InventoryItem existing = repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        int newQuantity = existing.getQuantity() + delta;
        if (newQuantity < 0) {
            log.warn("Rejected quantity adjust for id={}: delta={} would drop below 0 (current={})",
                    id, delta, existing.getQuantity());
            throw new IllegalArgumentException(
                    "Insufficient stock: current quantity is " + existing.getQuantity());
        }
        existing.setQuantity(newQuantity);
        InventoryItem saved = repository.save(existing);
        log.info("Adjusted quantity for id={} by {} -> {}", id, delta, newQuantity);
        return saved;
    }

    @CacheEvict(value = "items", key = "#id")
    public void deleteItem(Long id) {
        if (!repository.existsById(id)) {
            throw new ItemNotFoundException(id);
        }
        repository.deleteById(id);
        log.info("Deleted item id={}", id);
    }
}
