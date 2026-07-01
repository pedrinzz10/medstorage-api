package com.saas.MedStorage_api.consignment;

import com.saas.MedStorage_api.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ConsignmentControllerIntegrationTest {

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
                        .content("{\"nome\":\"Hospital Consignado\",\"email\":\"consig" + System.nanoTime() + "@teste.com\"}"))
                .andReturn().getResponse().getContentAsString();
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/consignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withGerenteRole_returns403() throws Exception {
        String customerId = newCustomerId();
        String productId = firstActiveProductId();
        mockMvc.perform(post("/api/consignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":5}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withVendedorRole_returns201AndDecrementsInventory() throws Exception {
        String customerId = newCustomerId();
        String productId = firstActiveProductId();
        String admin = adminToken();

        int antes = Integer.parseInt(
                mockMvc.perform(get("/api/inventory/" + productId).header("Authorization", "Bearer " + admin))
                        .andReturn().getResponse().getContentAsString()
                        .split("\"quantidadeAtual\":")[1].split("[,}]")[0].trim());

        String response = mockMvc.perform(post("/api/consignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":5}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ATIVO")))
                .andExpect(jsonPath("$.items[0].quantidadeEnviada", is(5)))
                .andExpect(jsonPath("$.items[0].saldoDisponivel", is(5)))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/inventory/" + productId).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.quantidadeAtual", is(antes - 5)));

        assertConsignmentIdPresent(response);
    }

    private void assertConsignmentIdPresent(String json) {
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"id\":"));
    }

    @Test
    void create_withInsufficientStock_returns400() throws Exception {
        String customerId = newCustomerId();
        String productId = firstActiveProductId();
        mockMvc.perform(post("/api/consignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":999999}]}"))
                .andExpect(status().isBadRequest());
    }

    private String[] createConsignmentWithItem(int quantidade) throws Exception {
        String customerId = newCustomerId();
        String productId = firstActiveProductId();
        String response = mockMvc.perform(post("/api/consignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId
                                + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantidade\":" + quantidade + "}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String consignmentId = response.split("\"id\":\"")[1].split("\"")[0];
        String itemId = response.split("\"items\":\\[\\{\"id\":\"")[1].split("\"")[0];
        return new String[] { consignmentId, itemId };
    }

    @Test
    void registerUsage_withVendedorRole_returns201AndReducesBalance() throws Exception {
        String[] ids = createConsignmentWithItem(10);
        mockMvc.perform(post("/api/consignments/" + ids[0] + "/items/" + ids[1] + "/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"quantidade\":4,\"dataUso\":\"2026-01-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidade", is(4)));
    }

    @Test
    void registerUsage_exceedingBalance_returns400() throws Exception {
        String[] ids = createConsignmentWithItem(3);
        mockMvc.perform(post("/api/consignments/" + ids[0] + "/items/" + ids[1] + "/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"quantidade\":10,\"dataUso\":\"2026-01-15\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturn_withVendedorRole_returns200AndRestoresInventory() throws Exception {
        String[] ids = createConsignmentWithItem(6);
        mockMvc.perform(post("/api/consignments/" + ids[0] + "/items/" + ids[1] + "/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"quantidade\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeDevolvida", is(2)))
                .andExpect(jsonPath("$.saldoDisponivel", is(4)));
    }

    @Test
    void fullConsumption_autoClosesConsignment() throws Exception {
        String[] ids = createConsignmentWithItem(5);
        mockMvc.perform(post("/api/consignments/" + ids[0] + "/items/" + ids[1] + "/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"quantidade\":5,\"dataUso\":\"2026-01-15\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/consignments/" + ids[0]).header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(jsonPath("$.status", is("ENCERRADO")));
    }

    @Test
    void close_withVendedorRole_returns403() throws Exception {
        String[] ids = createConsignmentWithItem(3);
        mockMvc.perform(patch("/api/consignments/" + ids[0] + "/close")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void close_withAdminRole_returns200() throws Exception {
        String[] ids = createConsignmentWithItem(3);
        mockMvc.perform(patch("/api/consignments/" + ids[0] + "/close")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ENCERRADO")));
    }
}
