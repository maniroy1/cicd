package com.example.inventory.controller;

import com.example.inventory.exception.ItemNotFoundException;
import com.example.inventory.model.InventoryItem;
import com.example.inventory.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Api-Key";
    private static final String VALID_KEY = "test-admin-key";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void search_returnsPagedResults() throws Exception {
        InventoryItem item = new InventoryItem("Laptop", "SKU-001", "Dell", 5,
                new BigDecimal("999.99"), "Electronics");
        Page<InventoryItem> page = new PageImpl<>(List.of(item));
        when(reportService.searchByName(eq("Lap"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/reports/search").param("name", "Lap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-001"));
    }

    @Test
    void averagePrice_whenNoItems_returnsZero() throws Exception {
        when(reportService.averagePrice()).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/api/v1/reports/average-price"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void summary_whenItemMissing_returns404() throws Exception {
        when(reportService.summary(99L)).thenThrow(new ItemNotFoundException(99L));

        mockMvc.perform(get("/api/v1/reports/99/summary"))
                .andExpect(status().isNotFound());
    }

    @Test
    void summary_returnsTotalValue() throws Exception {
        when(reportService.summary(1L)).thenReturn(Map.of("sku", "SKU-001",
                "totalValue", new BigDecimal("4999.95")));

        mockMvc.perform(get("/api/v1/reports/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void export_withoutApiKey_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/reports/export").param("file", "daily.csv"))
                .andExpect(status().isUnauthorized());

        verify(reportService, never()).readReport(any());
    }

    @Test
    void export_withValidKey_returnsContent() throws Exception {
        when(reportService.isAuthorizedAdmin(VALID_KEY)).thenReturn(true);
        when(reportService.readReport("daily.csv")).thenReturn(Optional.of("sku,qty"));

        mockMvc.perform(get("/api/v1/reports/export")
                        .param("file", "daily.csv")
                        .header(ADMIN_KEY_HEADER, VALID_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string("sku,qty"));
    }

    @Test
    void export_whenReportMissing_returns404() throws Exception {
        when(reportService.isAuthorizedAdmin(VALID_KEY)).thenReturn(true);
        when(reportService.readReport("nope.csv")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/reports/export")
                        .param("file", "nope.csv")
                        .header(ADMIN_KEY_HEADER, VALID_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    void export_withTraversalPath_returns400() throws Exception {
        when(reportService.isAuthorizedAdmin(VALID_KEY)).thenReturn(true);
        when(reportService.readReport("../../etc/passwd"))
                .thenThrow(new IllegalArgumentException("Invalid report file name"));

        mockMvc.perform(get("/api/v1/reports/export")
                        .param("file", "../../etc/passwd")
                        .header(ADMIN_KEY_HEADER, VALID_KEY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restore_withoutApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/reports/restore").param("backupHost", "backup-1"))
                .andExpect(status().isUnauthorized());

        verify(reportService, never()).startRestore(any());
    }

    @Test
    void restore_withValidKey_returns202() throws Exception {
        when(reportService.isAuthorizedAdmin(VALID_KEY)).thenReturn(true);

        mockMvc.perform(post("/api/v1/reports/restore")
                        .param("backupHost", "backup-1")
                        .header(ADMIN_KEY_HEADER, VALID_KEY))
                .andExpect(status().isAccepted());

        verify(reportService).startRestore("backup-1");
    }

    @Test
    void restore_withHostNotAllowListed_returns400() throws Exception {
        when(reportService.isAuthorizedAdmin(VALID_KEY)).thenReturn(true);
        doThrow(new IllegalArgumentException("Backup host is not allow-listed"))
                .when(reportService).startRestore("evil.example.com");

        mockMvc.perform(post("/api/v1/reports/restore")
                        .param("backupHost", "evil.example.com")
                        .header(ADMIN_KEY_HEADER, VALID_KEY))
                .andExpect(status().isBadRequest());
    }
}
