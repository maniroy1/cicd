package com.example.inventory.exception;

public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(Long id) {
        super("Inventory item not found with id: " + id);
    }

    public ItemNotFoundException(String message) {
        super(message);
    }
}
