package com.saas.MedStorage_api.inventory;

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
class InventoryControllerIntegrationTest {

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
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.quantidadeAtual").exists())
                .andExpect(jsonPath("$.disponivel").exists())
                .andExpect(jsonPath("$.reservada").exists())
                .andExpect(jsonPath("$.statusEstoque").exists());
    }

    @Test
    void getStatus_withToken_returnsDisponivelAndReservadaFields() throws Exception {
        mockMvc.perform(get("/api/inventory/status")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponivel").exists())
                .andExpect(jsonPath("$[0].reservada").exists())
                // seed: 0 reservations, so disponivel == quantidadeAtual
                .andExpect(jsonPath("$[0].reservada").value(0))
                .andExpect(jsonPath("$[0].disponivel").value(1000));
    }

    // ── POST /api/inventory/low-stock-alert/trigger ─────────────────────────────

    @Test
    void triggerLowStockAlert_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/inventory/low-stock-alert/trigger"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void triggerLowStockAlert_withVendedorRole_returns403() throws Exception {
        mockMvc.perform(post("/api/inventory/low-stock-alert/trigger")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void triggerLowStockAlert_withAdminRole_returns200() throws Exception {
        // seed: nenhum produto crítico, então nenhum email é enviado, mas o endpoint responde 200
        mockMvc.perform(post("/api/inventory/low-stock-alert/trigger")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtosCriticos").value(0));
    }
}
