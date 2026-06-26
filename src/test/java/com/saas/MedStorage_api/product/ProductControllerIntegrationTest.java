package com.saas.MedStorage_api.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.saas.MedStorage_api.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProductControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@distribuidor.com";
    private static final char[] ADMIN_SECRET = {'A', 'd', 'm', 'i', 'n', '1', '2', '3', '!'};

    @Autowired
    private MockMvc mockMvc;

    private String adminToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + new String(ADMIN_SECRET) + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return response.split("\"token\":\"")[1].split("\"")[0];
    }

    @Test
    void findAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findAll_withToken_returnsPagedActiveProducts() throws Exception {
        mockMvc.perform(get("/api/products?page=0&size=20")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void findById_withExistingProduct_returns200() throws Exception {
        String productsJson = mockMvc.perform(get("/api/products?page=0&size=1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        String productId = productsJson.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId));
    }

    @Test
    void findById_withUnknownProduct_returns404() throws Exception {
        mockMvc.perform(get("/api/products/" + java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }
}
