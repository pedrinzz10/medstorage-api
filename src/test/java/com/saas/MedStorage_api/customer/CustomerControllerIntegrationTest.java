package com.saas.MedStorage_api.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.saas.MedStorage_api.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CustomerControllerIntegrationTest {

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
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hospital Novo\",\"email\":\"compras@novo.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withValidPayload_returns201() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken())
                        .content("{\"nome\":\"Hospital Novo\",\"email\":\"compras@novo.com\",\"cnpj\":\"12345678901234\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Hospital Novo"))
                .andExpect(jsonPath("$.email").value("compras@novo.com"));
    }

    @Test
    void create_withoutEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken())
                        .content("{\"nome\":\"Clinica Invalida\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withoutNome_returns400() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken())
                        .content("{\"email\":\"compras@novo.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/customers/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrders_withExistingCustomer_returns200WithPagedOrders() throws Exception {
        String token = adminToken();

        String customerJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"nome\":\"Hospital Pedidos\",\"email\":\"pedidos@hospital.com\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String customerId = customerJson.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/customers/" + customerId + "/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getOrders_withUnknownCustomer_returns404() throws Exception {
        mockMvc.perform(get("/api/customers/" + UUID.randomUUID() + "/orders")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void fullLifecycle_createListGetUpdate() throws Exception {
        String token = adminToken();

        String createResponse = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"nome\":\"Clinica Vida\",\"email\":\"contato@clinicavida.com\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String customerId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/customers/" + customerId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Clinica Vida"));

        mockMvc.perform(put("/api/customers/" + customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"nome\":\"Clinica Vida Renomeada\",\"email\":\"novo@clinicavida.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Clinica Vida Renomeada"))
                .andExpect(jsonPath("$.email").value("novo@clinicavida.com"));
    }
}
