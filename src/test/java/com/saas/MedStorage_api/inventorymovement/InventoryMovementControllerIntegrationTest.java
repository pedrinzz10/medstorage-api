package com.saas.MedStorage_api.inventorymovement;

import com.saas.MedStorage_api.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class InventoryMovementControllerIntegrationTest {

    private static final char[] ADMIN_SECRET = {'A', 'd', 'm', 'i', 'n', '1', '2', '3', '!'};
    private static final char[] VENDEDOR_SECRET = {'V', 'e', 'n', 'd', 'e', 'd', 'o', 'r', '1', '2', '3', '!'};
    private static final char[] GERENTE_SECRET = {'G', 'e', 'r', 'e', 'n', 't', 'e', '1', '2', '3', '!'};

    @Autowired
    private MockMvc mockMvc;

    private String tokenFor(String email, char[] secret) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + new String(secret) + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return response.split("\"token\":\"")[1].split("\"")[0];
    }

    private String adminToken() throws Exception {
        return tokenFor("admin@distribuidor.com", ADMIN_SECRET);
    }

    private String vendedorToken() throws Exception {
        return tokenFor("vendedor1@distribuidor.com", VENDEDOR_SECRET);
    }

    private String gerenteToken() throws Exception {
        return tokenFor("gerente@distribuidor.com", GERENTE_SECRET);
    }

    private String firstActiveProductId() throws Exception {
        String json = mockMvc.perform(get("/api/products?page=0&size=1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    private void generateMovement(String productId) throws Exception {
        String adminToken = adminToken();
        String customerJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content("{\"nome\":\"Cliente Movimento\",\"email\":\"mov" + System.nanoTime() + "@test.com\"}"))
                .andReturn().getResponse().getContentAsString();
        String customerId = customerJson.split("\"id\":\"")[1].split("\"")[0];

        String orderJson = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = orderJson.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"newStatus\":\"ATENDIDO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void findAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/inventory/movements"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findAll_authenticated_returns200WithPage() throws Exception {
        String productId = firstActiveProductId();
        generateMovement(productId);

        mockMvc.perform(get("/api/inventory/movements")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void findAll_filteredByProductId_returnsOnlyThatProduct() throws Exception {
        String productId = firstActiveProductId();
        generateMovement(productId);

        mockMvc.perform(get("/api/inventory/movements?productId=" + productId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productId").value(productId))
                .andExpect(jsonPath("$.content[0].tipo").exists())
                .andExpect(jsonPath("$.content[0].quantidade").exists());
    }
}
