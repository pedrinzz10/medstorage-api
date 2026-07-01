package com.saas.MedStorage_api.consignment;

import com.saas.MedStorage_api.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ConsignmentCountControllerIntegrationTest {

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

    private String firstActiveProductId() throws Exception {
        String json = mockMvc.perform(get("/api/products?page=0&size=1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    private String newCustomerId() throws Exception {
        String json = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken())
                        .content("{\"nome\":\"Hospital Contagem\",\"email\":\"contagem" + System.nanoTime() + "@teste.com\"}"))
                .andReturn().getResponse().getContentAsString();
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    private String gerenteUserId() throws Exception {
        String json = mockMvc.perform(get("/api/users/staff")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andReturn().getResponse().getContentAsString();
        int idx = json.indexOf("\"email\":\"gerente@distribuidor.com\"");
        String before = json.substring(0, idx);
        return before.substring(before.lastIndexOf("\"id\":\"") + 6).split("\"")[0];
    }

    @Test
    void registerCount_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/consignments/counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerCount_withVendedorRole_returns403() throws Exception {
        String customerId = newCustomerId();
        String productId = firstActiveProductId();
        String consignmentJson = mockMvc.perform(post("/api/consignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":5}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String itemId = consignmentJson.split("\"items\":\\[\\{\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/consignments/counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId + "\",\"dataContagem\":\"2030-01-15\",\"items\":[{\"consignmentItemId\":\""
                                + itemId + "\",\"quantidadeContada\":5}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerCount_withMissingMaterial_returnsPositiveDivergenceAndMarksVisitRealizada() throws Exception {
        String customerId = newCustomerId();
        String productId = firstActiveProductId();
        String funcionarioId = gerenteUserId();

        // Envia 10 em consignação
        String consignmentJson = mockMvc.perform(post("/api/consignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":10}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String itemId = consignmentJson.split("\"items\":\\[\\{\"id\":\"")[1].split("\"")[0];

        // Reporta uso de 3 (saldo esperado passa a ser 7)
        mockMvc.perform(post("/api/consignments/" + consignmentJson.split("\"id\":\"")[1].split("\"")[0]
                        + "/items/" + itemId + "/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"quantidade\":3,\"dataUso\":\"2030-01-10\"}"))
                .andExpect(status().isCreated());

        // Agenda uma visita
        String visitJson = mockMvc.perform(post("/api/consignment-visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"customerId\":\"" + customerId + "\",\"funcionarioId\":\"" + funcionarioId
                                + "\",\"dataAgendada\":\"2030-01-15\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String visitId = visitJson.split("\"id\":\"")[1].split("\"")[0];

        // Contagem física encontra só 5 (esperado 7) -> divergencia = 2
        mockMvc.perform(post("/api/consignments/counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"customerId\":\"" + customerId + "\",\"visitId\":\"" + visitId
                                + "\",\"dataContagem\":\"2030-01-15\",\"items\":[{\"consignmentItemId\":\"" + itemId
                                + "\",\"quantidadeContada\":5,\"loteConferido\":\"L1\",\"validadeConferida\":\"2031-01-01\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].divergencia", is(2)));

        // Visita foi marcada como realizada
        mockMvc.perform(get("/api/consignment-visits?from=2030-01-01&to=2030-01-31")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(jsonPath("$[0].status", is("REALIZADA")));
    }

    @Test
    void findByCustomer_returnsCountHistory() throws Exception {
        String customerId = newCustomerId();
        String productId = firstActiveProductId();

        String consignmentJson = mockMvc.perform(post("/api/consignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":6}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String consignmentId = consignmentJson.split("\"id\":\"")[1].split("\"")[0];
        String itemId = consignmentJson.split("\"items\":\\[\\{\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/consignments/counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"customerId\":\"" + customerId + "\",\"dataContagem\":\"2030-02-01\",\"items\":[{\"consignmentItemId\":\""
                                + itemId + "\",\"quantidadeContada\":6}]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/consignments/counts?customerId=" + customerId)
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId", is(customerId)))
                .andExpect(jsonPath("$[0].items[0].divergencia", is(0)));

        org.junit.jupiter.api.Assertions.assertNotNull(consignmentId);
    }
}
