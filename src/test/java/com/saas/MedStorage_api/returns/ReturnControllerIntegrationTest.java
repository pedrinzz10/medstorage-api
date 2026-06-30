package com.saas.MedStorage_api.returns;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.saas.MedStorage_api.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ReturnControllerIntegrationTest {

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
        String json = mockMvc.perform(get("/api/products?page=0&size=1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    private String createCustomerId() throws Exception {
        String token = adminToken();
        String json = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"nome\":\"Cliente Devolução\",\"email\":\"dev@teste.com\"}"))
                .andReturn().getResponse().getContentAsString();
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    private String createFinalizadoOrderId(String customerId, String productId, int quantidade) throws Exception {
        String vendedorToken = vendedorToken();
        String gerenteToken = gerenteToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":" + quantidade + "}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"CONFIRMADO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"SEPARADO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"PRONTO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"FINALIZADO\"}"))
                .andExpect(status().isOk());

        return orderId;
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withFinalizadoOrder_returns201() throws Exception {
        String productId = firstActiveProductId();
        String customerId = createCustomerId();
        String orderId = createFinalizadoOrderId(customerId, productId, 5);

        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"orderId\":\"" + orderId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":2}]"
                                + ",\"motivo\":\"Produto com defeito\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.numeroRetorno").exists())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void create_withPendingOrder_returns400() throws Exception {
        String productId = firstActiveProductId();
        String customerId = createCustomerId();
        String vendedorToken = vendedorToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"orderId\":\"" + orderId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withNonExistentOrder_returns404() throws Exception {
        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"orderId\":\"" + UUID.randomUUID()
                                + "\",\"items\":[{\"productId\":\"" + UUID.randomUUID() + "\",\"quantidade\":1}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withQuantityExceedingOrder_returns400() throws Exception {
        String productId = firstActiveProductId();
        String customerId = createCustomerId();
        String orderId = createFinalizadoOrderId(customerId, productId, 2);

        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"orderId\":\"" + orderId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":5}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void process_withGerenteRole_returns200AndRestoresInventory() throws Exception {
        String productId = firstActiveProductId();
        String customerId = createCustomerId();
        String orderId = createFinalizadoOrderId(customerId, productId, 3);

        String inventoryBefore = mockMvc.perform(get("/api/inventory/" + productId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        int quantidadeAntes = Integer.parseInt(inventoryBefore.split("\"quantidadeAtual\":")[1].split(",")[0]);

        String createResponse = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"orderId\":\"" + orderId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":3}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String returnId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/returns/" + returnId + "/process")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSADO"))
                .andExpect(jsonPath("$.dataProcessamento").exists());

        String inventoryAfter = mockMvc.perform(get("/api/inventory/" + productId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        int quantidadeDepois = Integer.parseInt(inventoryAfter.split("\"quantidadeAtual\":")[1].split(",")[0]);

        org.junit.jupiter.api.Assertions.assertEquals(quantidadeAntes + 3, quantidadeDepois);
    }

    @Test
    void process_withVendedorRole_returns403() throws Exception {
        String productId = firstActiveProductId();
        String customerId = createCustomerId();
        String orderId = createFinalizadoOrderId(customerId, productId, 1);

        String createResponse = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"orderId\":\"" + orderId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String returnId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/returns/" + returnId + "/process")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void process_withAlreadyProcessedReturn_returns400() throws Exception {
        String productId = firstActiveProductId();
        String customerId = createCustomerId();
        String orderId = createFinalizadoOrderId(customerId, productId, 2);

        String createResponse = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"orderId\":\"" + orderId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        String returnId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/returns/" + returnId + "/process")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/returns/" + returnId + "/process")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reject_withPendingReturn_returns200WithRejectedStatus() throws Exception {
        String productId = firstActiveProductId();
        String customerId = createCustomerId();
        String orderId = createFinalizadoOrderId(customerId, productId, 2);

        String createResponse = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"orderId\":\"" + orderId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String returnId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/returns/" + returnId + "/reject")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADO"))
                .andExpect(jsonPath("$.dataProcessamento").exists());
    }

    @Test
    void reject_withAlreadyProcessedReturn_returns400() throws Exception {
        String productId = firstActiveProductId();
        String customerId = createCustomerId();
        String orderId = createFinalizadoOrderId(customerId, productId, 2);

        String createResponse = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"orderId\":\"" + orderId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String returnId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/returns/" + returnId + "/process")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/returns/" + returnId + "/reject")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reject_withVendedorRole_returns403() throws Exception {
        String productId = firstActiveProductId();
        String customerId = createCustomerId();
        String orderId = createFinalizadoOrderId(customerId, productId, 1);

        String createResponse = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"orderId\":\"" + orderId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String returnId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/returns/" + returnId + "/reject")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAll_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/returns")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isOk());
    }

    @Test
    void findById_withNonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/returns/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }
}
