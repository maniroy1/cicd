package com.example.inventory.controller;

import com.example.inventory.model.InventoryItem;
import com.example.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventoryItem> getAllItems() {
        return service.getAllItems();
    }

    @GetMapping("/{id}")
    public InventoryItem getItemById(@PathVariable Long id) {
        return service.getItemById(id);
    }

    @GetMapping("/sku/{sku}")
    public InventoryItem getItemBySku(@PathVariable String sku) {
        return service.getItemBySku(sku);
    }

    @GetMapping("/category/{category}")
    public List<InventoryItem> getItemsByCategory(@PathVariable String category) {
        return service.getItemsByCategory(category);
    }

    @GetMapping("/low-stock")
    public List<InventoryItem> getLowStockItems(@RequestParam(defaultValue = "10") Integer threshold) {
        return service.getLowStockItems(threshold);
    }

    @PostMapping
    public ResponseEntity<InventoryItem> createItem(@Valid @RequestBody InventoryItem item) {
        InventoryItem created = service.createItem(item);
        return ResponseEntity
                .created(URI.create("/api/v1/inventory/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public InventoryItem updateItem(@PathVariable Long id, @Valid @RequestBody InventoryItem item) {
        return service.updateItem(id, item);
    }

    @PatchMapping("/{id}/adjust-quantity")
    public InventoryItem adjustQuantity(@PathVariable Long id, @RequestParam int delta) {
        return service.adjustQuantity(id, delta);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        service.deleteItem(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
