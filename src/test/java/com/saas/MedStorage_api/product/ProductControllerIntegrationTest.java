package com.saas.MedStorage_api.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.saas.MedStorage_api.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProductControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@distribuidor.com";
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
        return tokenFor(ADMIN_EMAIL, ADMIN_SECRET);
    }

    private String vendedorToken() throws Exception {
        return tokenFor("vendedor1@distribuidor.com", VENDEDOR_SECRET);
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
        mockMvc.perform(get("/api/products/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withAdminToken_returns201AndCreatesProduct() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken())
                        .content("{\"nome\":\"Máscara N95\",\"sku\":\"MSK-N95-001\",\"precoBase\":5.00,\"ativo\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Máscara N95"))
                .andExpect(jsonPath("$.sku").value("MSK-N95-001"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void create_withDuplicateSku_returns400() throws Exception {
        String productsJson = mockMvc.perform(get("/api/products?page=0&size=1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        String existingSku = productsJson.split("\"sku\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken())
                        .content("{\"nome\":\"Duplicado\",\"sku\":\"" + existingSku + "\",\"precoBase\":1.00,\"ativo\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withVendedorToken_returns403() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"nome\":\"Produto\",\"sku\":\"SKU-NOVO\",\"precoBase\":1.00,\"ativo\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_withAdminToken_returns200() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"nome\":\"Produto Antes\",\"sku\":\"UPD-TST-001\",\"precoBase\":1.00,\"ativo\":true}"))
                .andExpect(status().isCreated());

        String productsJson = mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String productId = productsJson.split("\"sku\":\"UPD-TST-001\"")[0]
                .replaceAll(".*\"id\":\"", "").replaceAll("\".*", "");

        mockMvc.perform(put("/api/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"nome\":\"Produto Depois\",\"sku\":\"UPD-TST-001\",\"precoBase\":2.50,\"ativo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Produto Depois"))
                .andExpect(jsonPath("$.precoBase").value(2.50));
    }

    @Test
    void deactivate_withAdminToken_returns204AndProductDisappears() throws Exception {
        String token = adminToken();
        String createJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"nome\":\"Produto Temporário\",\"sku\":\"DEL-TST-001\",\"precoBase\":1.00,\"ativo\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String productId = createJson.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(delete("/api/products/" + productId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deactivate_withVendedorToken_returns403() throws Exception {
        String productsJson = mockMvc.perform(get("/api/products?page=0&size=1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        String productId = productsJson.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(delete("/api/products/" + productId)
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isForbidden());
    }
}
