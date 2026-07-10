package com.example.inventory;

import com.example.inventory.model.InventoryItem;
import com.example.inventory.repository.InventoryItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryItemRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void contextLoads() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void fullCrudLifecycle() throws Exception {
        InventoryItem newItem = new InventoryItem("Keyboard", "SKU-100", "Mechanical keyboard",
                50, new BigDecimal("89.99"), "Electronics");

        // Create
        MvcResult createResult = mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sku").value("SKU-100"))
                .andReturn();

        InventoryItem created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), InventoryItem.class);
        Long id = created.getId();

        // Read
        mockMvc.perform(get("/api/v1/inventory/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keyboard"));

        // Update
        created.setQuantity(75);
        created.setPrice(new BigDecimal("79.99"));
        mockMvc.perform(put("/api/v1/inventory/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(75))
                .andExpect(jsonPath("$.price").value(79.99));

        // Delete
        mockMvc.perform(delete("/api/v1/inventory/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/inventory/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createItem_withDuplicateSku_returns409() throws Exception {
        InventoryItem first = new InventoryItem("Monitor", "SKU-200", "4K monitor",
                10, new BigDecimal("349.99"), "Electronics");

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        InventoryItem duplicate = new InventoryItem("Another Monitor", "SKU-200", "Duplicate SKU",
                5, new BigDecimal("299.99"), "Electronics");

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "An inventory item with SKU 'SKU-200' already exists"));
    }
}
