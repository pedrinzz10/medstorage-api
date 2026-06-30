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
    void fullFlow_createOrder_thenAdvanceThroughAllStatuses_updatesStockCorrectly() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();
        String gerenteToken = gerenteToken();

        // 1. Criar pedido → CRIADO
        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":10}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CRIADO"))
                .andExpect(jsonPath("$.numeroPedido").exists())
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        // 2. Confirmar pagamento → CONFIRMADO
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"CONFIRMADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.dataConfirmado").exists());

        // captura estoque total antes de SEPARADO
        String beforeSeparado = mockMvc.perform(get("/api/inventory/" + productId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andReturn().getResponse().getContentAsString();
        int totalAntes = Integer.parseInt(beforeSeparado.split("\"quantidadeAtual\":")[1].split(",")[0]);
        int disponivelAntes = Integer.parseInt(beforeSeparado.split("\"disponivel\":")[1].split(",")[0]);

        // 3. Separar itens → SEPARADO (reserva estoque, nao decrementa total)
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"SEPARADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SEPARADO"))
                .andExpect(jsonPath("$.dataSeparado").exists());

        // verifica reserva: total nao muda, disponivel diminui, reservada aumenta
        String afterSeparado = mockMvc.perform(get("/api/inventory/" + productId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andReturn().getResponse().getContentAsString();
        int totalAposSeparado = Integer.parseInt(afterSeparado.split("\"quantidadeAtual\":")[1].split(",")[0]);
        int reservadaAposSeparado = Integer.parseInt(afterSeparado.split("\"reservada\":")[1].split(",")[0]);

        org.junit.jupiter.api.Assertions.assertEquals(totalAntes, totalAposSeparado);
        org.junit.jupiter.api.Assertions.assertEquals(10, reservadaAposSeparado);

        // 4. Marcar como PRONTO → cliente e staff recebem email (capturado/logado se falhar)
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"PRONTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRONTO"))
                .andExpect(jsonPath("$.dataPronte").exists());

        // 5. Finalizar → FINALIZADO (decrementa total e reserva, acumula comissao)
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"FINALIZADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"))
                .andExpect(jsonPath("$.dataFinalizado").exists());

        // verifica: total decrementou, reservada voltou a 0
        String afterFinalizado = mockMvc.perform(get("/api/inventory/" + productId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andReturn().getResponse().getContentAsString();
        int totalAposFinalizado = Integer.parseInt(afterFinalizado.split("\"quantidadeAtual\":")[1].split(",")[0]);
        int reservadaAposFinalizado = Integer.parseInt(afterFinalizado.split("\"reservada\":")[1].split(",")[0]);

        org.junit.jupiter.api.Assertions.assertEquals(totalAntes - 10, totalAposFinalizado);
        org.junit.jupiter.api.Assertions.assertEquals(0, reservadaAposFinalizado);
    }

    @Test
    void changeStatus_toFinalizadoDirectlyFromCriado_returns400() throws Exception {
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
                        .content("{\"newStatus\":\"FINALIZADO\"}"))
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

        mockMvc.perform(get("/api/orders?status=CRIADO&page=0&size=50")
                        .header("Authorization", "Bearer " + vendedorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("CRIADO"));
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
    void changeStatus_toSeparado_withInsufficientStock_returns400() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();
        String gerenteToken = gerenteToken();

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
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"CONFIRMADO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"SEPARADO\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + vendedorToken))
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
    }

    @Test
    void changeStatus_toCancelado_fromSeparado_releasesReservation() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();
        String gerenteToken = gerenteToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":5}]}"))
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

        // verifica que reserva foi criada
        String afterSeparado = mockMvc.perform(get("/api/inventory/" + productId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andReturn().getResponse().getContentAsString();
        int reservadaAntes = Integer.parseInt(afterSeparado.split("\"reservada\":")[1].split(",")[0]);
        org.junit.jupiter.api.Assertions.assertTrue(reservadaAntes >= 5);

        // cancelar
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"CANCELADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        // verifica que reserva foi liberada
        String afterCancelado = mockMvc.perform(get("/api/inventory/" + productId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andReturn().getResponse().getContentAsString();
        int reservadaDepois = Integer.parseInt(afterCancelado.split("\"reservada\":")[1].split(",")[0]);
        org.junit.jupiter.api.Assertions.assertEquals(reservadaAntes - 5, reservadaDepois);
    }

    @Test
    void delete_withCriadoOrder_returns204() throws Exception {
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
    void delete_withConfirmadoOrder_returns400() throws Exception {
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
                        .content("{\"newStatus\":\"CONFIRMADO\"}"))
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
    void update_withCriadoOrder_returns200WithNewValues() throws Exception {
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
                .andExpect(jsonPath("$.status").value("CRIADO"))
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
    void findAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findById_withExistingOrder_returns200() throws Exception {
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

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("CRIADO"))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void findById_withUnknownOrder_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_withConfirmadoOrder_returns400() throws Exception {
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
                        .content("{\"newStatus\":\"CONFIRMADO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":2}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_toCancelado_fromCriado_returns200() throws Exception {
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
                        .content("{\"newStatus\":\"CANCELADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        mockMvc.perform(get("/api/inventory/" + productId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andExpect(jsonPath("$.reservada").value(0));
    }

    @Test
    void changeStatus_toCancelado_fromPronte_releasesReservation() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();
        String gerenteToken = gerenteToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":5}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        for (String s : new String[]{"CONFIRMADO", "SEPARADO", "PRONTO"}) {
            mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + gerenteToken)
                            .content("{\"newStatus\":\"" + s + "\"}"))
                    .andExpect(status().isOk());
        }

        String inventoryJson = mockMvc.perform(get("/api/inventory/" + productId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andReturn().getResponse().getContentAsString();
        int reservadaAntes = Integer.parseInt(inventoryJson.split("\"reservada\":")[1].split(",")[0]);

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"CANCELADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        String inventoryAfter = mockMvc.perform(get("/api/inventory/" + productId)
                        .header("Authorization", "Bearer " + vendedorToken))
                .andReturn().getResponse().getContentAsString();
        int reservadaDepois = Integer.parseInt(inventoryAfter.split("\"reservada\":")[1].split(",")[0]);

        org.junit.jupiter.api.Assertions.assertEquals(reservadaAntes - 5, reservadaDepois);
    }

    @Test
    void changeStatus_toCancelado_fromFinalizado_returns400() throws Exception {
        String customerId = firstCustomerId();
        String productId = firstActiveProductId();
        String vendedorToken = vendedorToken();
        String gerenteToken = gerenteToken();

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        String orderId = createResponse.split("\"id\":\"")[1].split("\"")[0];

        for (String s : new String[]{"CONFIRMADO", "SEPARADO", "PRONTO", "FINALIZADO"}) {
            mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + gerenteToken)
                            .content("{\"newStatus\":\"" + s + "\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken)
                        .content("{\"newStatus\":\"CANCELADO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_withInvalidStatus_returns400() throws Exception {
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
                        .content("{\"newStatus\":\"PENDENTE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_toConfirmado_withVendedorRole_returns403() throws Exception {
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
                        .content("{\"newStatus\":\"CONFIRMADO\"}"))
                .andExpect(status().isForbidden());
    }
}
