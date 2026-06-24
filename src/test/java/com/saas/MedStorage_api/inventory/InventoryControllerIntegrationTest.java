package com.saas.MedStorage_api.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryControllerIntegrationTest {

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
    void getStatus_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/inventory/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStatus_withToken_returnsAllProductsAsOk() throws Exception {
        // seed: 5 produtos com 1000 unidades cada, muito acima do estoque_minimo
        mockMvc.perform(get("/api/inventory/status")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].statusEstoque").value("OK"))
                .andExpect(jsonPath("$[0].quantidadeAtual").value(1000));
    }

    @Test
    void findByProductId_withUnknownProduct_returns404() throws Exception {
        mockMvc.perform(get("/api/inventory/" + java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findByProductId_withExistingProduct_returns200() throws Exception {
        String statusJson = mockMvc.perform(get("/api/inventory/status")
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        String productId = statusJson.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/inventory/" + productId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId));
    }
}
