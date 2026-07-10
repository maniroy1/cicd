package com.example.inventory.controller;

import com.example.inventory.exception.ItemNotFoundException;
import com.example.inventory.model.InventoryItem;
import com.example.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService service;

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        item = new InventoryItem("Laptop", "SKU-001", "Dell XPS 15", 25,
                new BigDecimal("1299.99"), "Electronics");
        item.setId(1L);
    }

    @Test
    void getAllItems_returnsOkWithList() throws Exception {
        when(service.getAllItems()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].sku").value("SKU-001"));
    }

    @Test
    void getItemById_whenExists_returnsOk() throws Exception {
        when(service.getItemById(1L)).thenReturn(item);

        mockMvc.perform(get("/api/v1/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void getItemById_whenNotFound_returns404() throws Exception {
        when(service.getItemById(99L)).thenThrow(new ItemNotFoundException(99L));

        mockMvc.perform(get("/api/v1/inventory/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Inventory item not found with id: 99"));
    }

    @Test
    void getItemBySku_returnsOk() throws Exception {
        when(service.getItemBySku("SKU-001")).thenReturn(item);

        mockMvc.perform(get("/api/v1/inventory/sku/SKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void getLowStockItems_returnsOk() throws Exception {
        when(service.getLowStockItems(10)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/inventory/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void createItem_withValidBody_returns201() throws Exception {
        when(service.createItem(any(InventoryItem.class))).thenReturn(item);

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void createItem_withMissingName_returns400() throws Exception {
        InventoryItem invalid = new InventoryItem(null, "SKU-002", "No name", 5,
                new BigDecimal("9.99"), "Misc");

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createItem_withNegativeQuantity_returns400() throws Exception {
        InventoryItem invalid = new InventoryItem("Widget", "SKU-003", "Bad qty", -5,
                new BigDecimal("9.99"), "Misc");

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").exists());
    }

    @Test
    void updateItem_returnsUpdatedItem() throws Exception {
        when(service.updateItem(eq(1L), any(InventoryItem.class))).thenReturn(item);

        mockMvc.perform(put("/api/v1/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void adjustQuantity_returnsUpdatedItem() throws Exception {
        item.setQuantity(30);
        when(service.adjustQuantity(1L, 5)).thenReturn(item);

        mockMvc.perform(patch("/api/v1/inventory/1/adjust-quantity")
                        .param("delta", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(30));
    }

    @Test
    void deleteItem_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/1"))
                .andExpect(status().isNoContent());
        verify(service).deleteItem(1L);
    }

    @Test
    void deleteItem_whenNotFound_returns404() throws Exception {
        doThrow(new ItemNotFoundException(99L)).when(service).deleteItem(99L);

        mockMvc.perform(delete("/api/v1/inventory/99"))
                .andExpect(status().isNotFound());
    }
}
