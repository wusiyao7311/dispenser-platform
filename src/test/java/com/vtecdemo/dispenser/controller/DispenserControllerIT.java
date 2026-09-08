package com.vtecdemo.dispenser.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack slice against the "test" profile's in-memory H2 database
 * (see application.yml) — exercises controller -> service -> repository ->
 * JPA end to end, the same layering used in production against Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DispenserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createThenFetchDispenser_roundTripsThroughRealDatabase() throws Exception {
        String payload = """
                {"code":"DSP-IT-01","location":"Test Site","status":"ACTIVE"}
                """;

        mockMvc.perform(post("/api/dispensers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("DSP-IT-01")));

        mockMvc.perform(get("/api/dispensers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'DSP-IT-01')]").exists());
    }

    @Test
    void createDispenser_missingLocation_returnsBadRequest() throws Exception {
        String payload = """
                {"code":"DSP-IT-02","status":"ACTIVE"}
                """;

        mockMvc.perform(post("/api/dispensers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUnknownDispenser_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/dispensers/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void restockBeyondCapacity_isCappedNotOverfilled() throws Exception {
        String dispenserPayload = """
                {"code":"DSP-IT-03","location":"Test Site","status":"ACTIVE"}
                """;
        String dispenserResponse = mockMvc.perform(post("/api/dispensers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dispenserPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number dispenserId = com.jayway.jsonpath.JsonPath.read(dispenserResponse, "$.id");

        String productPayload = """
                {"sku":"SKU-IT-03","name":"Test Product","unitPrice":1.00}
                """;
        String productResponse = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number productId = com.jayway.jsonpath.JsonPath.read(productResponse, "$.id");

        String restockPayload = String.format("""
                {"productId":%d,"quantity":100,"capacity":10}
                """, productId.longValue());

        mockMvc.perform(post("/api/dispensers/{id}/restock", dispenserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restockPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(10)))
                .andExpect(jsonPath("$.capacity", is(10)));
    }
}
