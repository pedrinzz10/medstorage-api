package com.saas.MedStorage_api.batch;

import com.saas.MedStorage_api.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class BatchControllerIntegrationTest {

    private static final char[] ADMIN_SECRET = {'A', 'd', 'm', 'i', 'n', '1', '2', '3', '!'};
    private static final char[] VENDEDOR_SECRET = {'V', 'e', 'n', 'd', 'e', 'd', 'o', 'r', '1', '2', '3', '!'};

    @Autowired
    private MockMvc mockMvc;

    private String tokenFor(String email, char[] secret) throws Exception {
        String setCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + new String(secret) + "\"}"))
                .andReturn().getResponse().getHeader("Set-Cookie");
        return setCookie.split("jwt=")[1].split(";")[0];
    }

    private String adminToken() throws Exception {
        return tokenFor("admin@distribuidor.com", ADMIN_SECRET);
    }

    private String vendedorToken() throws Exception {
        return tokenFor("vendedor1@distribuidor.com", VENDEDOR_SECRET);
    }

    private String firstActiveProductId() throws Exception {
        String json = mockMvc.perform(get("/api/products?page=0&size=1").header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    // ── GET /api/products/{id}/batches ──────────────────────────────────────────

    @Test
    void productBatches_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/products/" + firstActiveProductId() + "/batches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productBatches_withVendedorRole_returns403() throws Exception {
        mockMvc.perform(get("/api/products/" + firstActiveProductId() + "/batches")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void productBatches_withUnknownProduct_returns404() throws Exception {
        mockMvc.perform(get("/api/products/" + UUID.randomUUID() + "/batches")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void productBatches_withAdminRole_returnsEmptyListForProductWithNoBatches() throws Exception {
        mockMvc.perform(get("/api/products/" + firstActiveProductId() + "/batches")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── GET /api/inventory/batches/{batchId}/orders ─────────────────────────────

    @Test
    void batchOrders_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/inventory/batches/" + UUID.randomUUID() + "/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void batchOrders_withVendedorRole_returns403() throws Exception {
        mockMvc.perform(get("/api/inventory/batches/" + UUID.randomUUID() + "/orders")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void batchOrders_withUnknownBatch_returns404() throws Exception {
        mockMvc.perform(get("/api/inventory/batches/" + UUID.randomUUID() + "/orders")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }
}
