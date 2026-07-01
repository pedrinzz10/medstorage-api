package com.saas.MedStorage_api.batch;

import com.saas.MedStorage_api.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o ciclo completo de alocação FEFO: produto novo recebe dois lotes
 * com validades diferentes, um pedido consome quantidade que atravessa os
 * dois lotes (rateio), e o cancelamento devolve as quantidades alocadas.
 */
@IntegrationTest
class BatchAllocationIntegrationTest {

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
    void fullFefoCycle_splitsAcrossBatches_tracesOrder_andReleasesOnCancel() throws Exception {
        String admin = adminToken();
        String vendedor = vendedorToken();
        String gerente = gerenteToken();

        // Produto novo, sem lotes ainda
        String productJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + admin)
                        .content("{\"nome\":\"Produto FEFO Teste\",\"sku\":\"FEFO-" + System.nanoTime()
                                + "\",\"precoBase\":10.00,\"unidade\":\"unidade\",\"estoqueMinimo\":1,\"ativo\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String productId = productJson.split("\"id\":\"")[1].split("\"")[0];

        // Lote que vence antes (3 unidades)
        mockMvc.perform(post("/api/inventory/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + admin)
                        .content("{\"productId\":\"" + productId + "\",\"quantidade\":3,\"motivo\":\"Recebimento 1\","
                                + "\"lote\":\"LOTE-CEDO\",\"validade\":\"2030-01-01\"}"))
                .andExpect(status().isCreated());

        // Lote que vence depois (10 unidades)
        mockMvc.perform(post("/api/inventory/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + admin)
                        .content("{\"productId\":\"" + productId + "\",\"quantidade\":10,\"motivo\":\"Recebimento 2\","
                                + "\"lote\":\"LOTE-TARDE\",\"validade\":\"2031-01-01\"}"))
                .andExpect(status().isCreated());

        // Cliente + pedido de 5 unidades (deve consumir 3 do LOTE-CEDO + 2 do LOTE-TARDE)
        String customerJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + admin)
                        .content("{\"nome\":\"Cliente FEFO\",\"email\":\"fefo" + System.nanoTime() + "@teste.com\"}"))
                .andReturn().getResponse().getContentAsString();
        String customerId = customerJson.split("\"id\":\"")[1].split("\"")[0];

        String orderJson = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedor)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":5}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = orderJson.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerente)
                        .content("{\"newStatus\":\"CONFIRMADO\"}"))
                .andExpect(status().isOk());

        // SEPARADO aciona a alocação FEFO
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerente)
                        .content("{\"newStatus\":\"SEPARADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lotes", hasSize(2)))
                .andExpect(jsonPath("$.items[0].lotes[0].lote").value("LOTE-CEDO"))
                .andExpect(jsonPath("$.items[0].lotes[0].quantidadeConsumida").value(3))
                .andExpect(jsonPath("$.items[0].lotes[1].lote").value("LOTE-TARDE"))
                .andExpect(jsonPath("$.items[0].lotes[1].quantidadeConsumida").value(2));

        // Lotes refletem o consumo
        String batchesJson = mockMvc.perform(get("/api/products/" + productId + "/batches")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.lote == 'LOTE-CEDO')].quantidade").value(contains(0)))
                .andExpect(jsonPath("$[?(@.lote == 'LOTE-TARDE')].quantidade").value(contains(8)))
                .andReturn().getResponse().getContentAsString();
        String loteCedoId = batchesJson.split("\"lote\":\"LOTE-CEDO\"")[0]
                .replaceAll(".*\"id\":\"", "").replaceAll("\".*", "");

        // Rastreabilidade reversa: o pedido aparece na consulta do lote consumido
        mockMvc.perform(get("/api/inventory/batches/" + loteCedoId + "/orders")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quantidadeConsumida").value(3));

        // Cancelar o pedido devolve as quantidades aos lotes originais
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerente)
                        .content("{\"newStatus\":\"CANCELADO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/" + productId + "/batches")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.lote == 'LOTE-CEDO')].quantidade").value(contains(3)))
                .andExpect(jsonPath("$[?(@.lote == 'LOTE-TARDE')].quantidade").value(contains(10)));
    }

    @Test
    void allocateFefo_withInsufficientBatchStock_blocksSeparadoTransition() throws Exception {
        String admin = adminToken();
        String vendedor = vendedorToken();
        String gerente = gerenteToken();

        String productJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + admin)
                        .content("{\"nome\":\"Produto FEFO Insuficiente\",\"sku\":\"FEFO-INS-" + System.nanoTime()
                                + "\",\"precoBase\":10.00,\"unidade\":\"unidade\",\"estoqueMinimo\":1,\"ativo\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String productId = productJson.split("\"id\":\"")[1].split("\"")[0];

        // Estoque agregado permite 20 (reserva passa), mas o único lote só tem 5
        mockMvc.perform(post("/api/inventory/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + admin)
                        .content("{\"productId\":\"" + productId + "\",\"quantidade\":5,\"motivo\":\"Recebimento\","
                                + "\"lote\":\"LOTE-UNICO\",\"validade\":\"2030-01-01\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/inventory/movements/count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + admin)
                        .content("{\"productId\":\"" + productId + "\",\"quantidadeContada\":20,\"observacao\":\"Ajuste manual sem lote\"}"))
                .andExpect(status().isCreated());

        String customerJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + admin)
                        .content("{\"nome\":\"Cliente FEFO Insuf\",\"email\":\"fefoins" + System.nanoTime() + "@teste.com\"}"))
                .andReturn().getResponse().getContentAsString();
        String customerId = customerJson.split("\"id\":\"")[1].split("\"")[0];

        String orderJson = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedor)
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":20}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = orderJson.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerente)
                        .content("{\"newStatus\":\"CONFIRMADO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerente)
                        .content("{\"newStatus\":\"SEPARADO\"}"))
                .andExpect(status().isBadRequest());
    }
}
