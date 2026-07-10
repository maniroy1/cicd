package com.example.inventory.exception;

public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("An inventory item with SKU '" + sku + "' already exists");
    }
}
