package com.saas.MedStorage_api.commission;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.saas.MedStorage_api.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CommissionControllerIntegrationTest {

    private static final char[] ADMIN_SECRET = {'A', 'd', 'm', 'i', 'n', '1', '2', '3', '!'};
    private static final char[] VENDEDOR_SECRET = {'V', 'e', 'n', 'd', 'e', 'd', 'o', 'r', '1', '2', '3', '!'};
    private static final char[] GERENTE_SECRET = {'G', 'e', 'r', 'e', 'n', 't', 'e', '1', '2', '3', '!'};

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

    private String gerenteToken() throws Exception {
        return tokenFor("gerente@distribuidor.com", GERENTE_SECRET);
    }

    @Test
    void findAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/commissions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findAll_withVendedorToken_returns403() throws Exception {
        mockMvc.perform(get("/api/commissions")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAll_withAdminToken_returns200WithEmptyPage() throws Exception {
        mockMvc.perform(get("/api/commissions")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void findAll_withStatusFilter_returns200() throws Exception {
        mockMvc.perform(get("/api/commissions?status=PENDENTE")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void findAll_afterFinalizadoOrder_returnsCommissionEntry() throws Exception {
        String adminToken = adminToken();
        String vendedorToken = vendedorToken();
        String gerenteToken = gerenteToken();

        String customerJson = mockMvc.perform(post("/api/customers")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content("{\"nome\":\"Cliente Comissao\",\"email\":\"com" + System.nanoTime() + "@teste.com\"}"))
                .andReturn().getResponse().getContentAsString();
        String customerId = customerJson.split("\"id\":\"")[1].split("\"")[0];

        String productJson = mockMvc.perform(get("/api/products?page=0&size=1")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();
        String productId = productJson.split("\"id\":\"")[1].split("\"")[0];

        String orderJson = mockMvc.perform(post("/api/orders")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":2}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = orderJson.split("\"id\":\"")[1].split("\"")[0];

        for (String s : new String[]{"CONFIRMADO", "SEPARADO", "PRONTO", "FINALIZADO"}) {
            mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + gerenteToken)
                            .content("{\"newStatus\":\"" + s + "\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/commissions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(
                        org.hamcrest.Matchers.greaterThan(0)));
    }
}
