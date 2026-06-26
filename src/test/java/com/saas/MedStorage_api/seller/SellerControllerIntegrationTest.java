package com.saas.MedStorage_api.seller;

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
class SellerControllerIntegrationTest {

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

    private String vendedorToken() throws Exception {
        return tokenFor("vendedor1@distribuidor.com", VENDEDOR_SECRET);
    }

    private String gerenteToken() throws Exception {
        return tokenFor("gerente@distribuidor.com", GERENTE_SECRET);
    }

    private String adminToken() throws Exception {
        return tokenFor("admin@distribuidor.com", ADMIN_SECRET);
    }

    @Test
    void getMyPerformance_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/sellers/performance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyPerformance_withGerenteToken_returns403() throws Exception {
        mockMvc.perform(get("/api/sellers/performance")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyPerformance_withVendedorToken_returnsStructuredResponse() throws Exception {
        mockMvc.perform(get("/api/sellers/performance")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendedorId").exists())
                .andExpect(jsonPath("$.vendedorNome").exists())
                .andExpect(jsonPath("$.totalPedidos").exists())
                .andExpect(jsonPath("$.valorVendido").exists())
                .andExpect(jsonPath("$.quantidadeUnidades").exists());
    }

    @Test
    void getMyPerformance_withAdminToken_returns200() throws Exception {
        mockMvc.perform(get("/api/sellers/performance")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void getAllPerformance_withAdminToken_returns200() throws Exception {
        mockMvc.perform(get("/api/sellers/performance/all")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void getAllPerformance_withVendedorToken_returns403() throws Exception {
        mockMvc.perform(get("/api/sellers/performance/all")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isForbidden());
    }
}
