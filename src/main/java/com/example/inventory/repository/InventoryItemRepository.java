package com.example.inventory.repository;

import com.example.inventory.model.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findBySku(String sku);

    Page<InventoryItem> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT AVG(i.price) FROM InventoryItem i")
    Double findAveragePrice();

    List<InventoryItem> findByCategoryIgnoreCase(String category);

    List<InventoryItem> findByQuantityLessThan(Integer threshold);

    boolean existsBySku(String sku);
}
