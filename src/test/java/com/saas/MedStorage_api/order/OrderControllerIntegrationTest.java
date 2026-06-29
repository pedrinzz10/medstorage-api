package com.saas.MedStorage_api.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.saas.MedStorage_api.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sem servidor SMTP configurado no ambiente de teste, o envio de email
 * falha (timeout/conexao recusada) mas e capturado e logado pelo
 * OrderNotificationService - por isso nao precisa de mock aqui, o teste
 * valida que o fluxo principal (status + estoque) funciona independente
 * do email.
 */
@IntegrationTest
class OrderControllerIntegrationTest {

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

    private String vendedorToken() throws Exception {
        return tokenFor("vendedor1@distribuidor.com", VENDEDOR_SECRET);
    }

    private String gerenteToken() throws Exception {
        return tokenFor("gerente@distribuidor.com", GERENTE_SECRET);
    }

    private String adminToken() throws Exception {
        return tokenFor("admin@distribuidor.com", ADMIN_SECRET);
    }

    private String firstActiveProductId() throws Exception {
        String json = mockMvc.perform(get("/api/products?page=0&size=1").header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    private String firstCustomerId() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{\"nome\":\"Hospital Pedido Teste\",\"email\":\"pedido@teste.com\"}"));

        String json = mockMvc.perform(get("/api/customers?page=0&size=1").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withGerenteRole_returns403() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutItems_returns400() throws Exception {
        String customerId = firstCustomerId();
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId + "\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withUnknownCustomer_returns404() throws Exception {
        String productId = firstActiveProductId();
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + UUID.randomUUID()
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void fullFlow_createOrder_thenMarkAsAttended_decrementsStock() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":10}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.numeroPedido").exists())
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + vendedorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        String beforeStatus = mockMvc.perform(get("/api/inventory/" + productId).header("Authorization", "Bearer " + vendedorToken))
                .andReturn().getResponse().getContentAsString();
        int quantidadeAntes = Integer.parseInt(beforeStatus.split("\"quantidadeAtual\":")[1].split(",")[0]);

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"newStatus\":\"ATENDIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATENDIDO"))
                .andExpect(jsonPath("$.dataAtendimento").exists());

        String afterStatus = mockMvc.perform(get("/api/inventory/" + productId).header("Authorization", "Bearer " + vendedorToken))
                .andReturn().getResponse().getContentAsString();
        int quantidadeDepois = Integer.parseInt(afterStatus.split("\"quantidadeAtual\":")[1].split(",")[0]);

        org.junit.jupiter.api.Assertions.assertEquals(quantidadeAntes - 10, quantidadeDepois);

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"newStatus\":\"RETIRADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETIRADO"))
                .andExpect(jsonPath("$.dataRetirada").exists());
    }

    @Test
    void markAsWithdrawn_withPendingOrder_returns400() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"newStatus\":\"RETIRADO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_filterByStatus_returnsOnlyMatchingOrders() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + vendedorToken)
                .content("{\"customerId\":\"" + customerId
                        + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"));

        mockMvc.perform(get("/api/orders?status=PENDENTE&page=0&size=50")
                        .header("Authorization", "Bearer " + vendedorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDENTE"));
    }

    @Test
    void findAll_filterByCustomerId_returnsOnlyThatCustomersOrders() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + vendedorToken)
                .content("{\"customerId\":\"" + customerId
                        + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"));

        mockMvc.perform(get("/api/orders?customerId=" + customerId + "&page=0&size=50")
                        .header("Authorization", "Bearer " + vendedorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerId").value(customerId));
    }

    @Test
    void markAsAttended_withInsufficientStock_returns400() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":999999}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"newStatus\":\"ATENDIDO\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + vendedorToken))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void delete_withPendingOrder_returns204() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(delete("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_withAttendedOrder_returns400() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"newStatus\":\"ATENDIDO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_withGerenteRole_returns403() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(delete("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_withPendingOrder_returns200WithNewValues() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":2}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(put("/api/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":5}]"
                                + ",\"notas\":\"atualizado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.notas").value("atualizado"))
                .andExpect(jsonPath("$.items[0].quantidade").value(5));
    }

    @Test
    void update_withGerenteRole_returns403() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(put("/api/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void markAsAttended_withVendedorRole_returns403() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"newStatus\":\"ATENDIDO\"}"))
                .andExpect(status().isForbidden());
    }
}
