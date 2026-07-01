package com.saas.MedStorage_api.consignment;

import com.saas.MedStorage_api.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ConsignmentVisitControllerIntegrationTest {

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

    private String newCustomerId() throws Exception {
        String json = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken())
                        .content("{\"nome\":\"Hospital Visita\",\"email\":\"visita" + System.nanoTime() + "@teste.com\"}"))
                .andReturn().getResponse().getContentAsString();
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    private String gerenteUserId() throws Exception {
        String json = mockMvc.perform(get("/api/users?page=0&size=100")
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        int idx = json.indexOf("\"email\":\"gerente@distribuidor.com\"");
        String before = json.substring(0, idx);
        return before.substring(before.lastIndexOf("\"id\":\"") + 6).split("\"")[0];
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/consignment-visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withVendedorRole_returns403() throws Exception {
        String customerId = newCustomerId();
        String funcionarioId = gerenteUserId();
        mockMvc.perform(post("/api/consignment-visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"customerId\":\"" + customerId + "\",\"funcionarioId\":\"" + funcionarioId
                                + "\",\"dataAgendada\":\"2030-06-15\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withGerenteRole_returns201() throws Exception {
        String customerId = newCustomerId();
        String funcionarioId = gerenteUserId();
        mockMvc.perform(post("/api/consignment-visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"customerId\":\"" + customerId + "\",\"funcionarioId\":\"" + funcionarioId
                                + "\",\"dataAgendada\":\"2030-06-15\",\"observacoes\":\"Levar balança\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("AGENDADA")))
                .andExpect(jsonPath("$.dataAgendada", is("2030-06-15")));
    }

    @Test
    void findByRange_returnsVisitsWithinRange() throws Exception {
        String customerId = newCustomerId();
        String funcionarioId = gerenteUserId();
        mockMvc.perform(post("/api/consignment-visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"customerId\":\"" + customerId + "\",\"funcionarioId\":\"" + funcionarioId
                                + "\",\"dataAgendada\":\"2030-07-10\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/consignment-visits?from=2030-07-01&to=2030-07-31")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dataAgendada", is("2030-07-10")));

        mockMvc.perform(get("/api/consignment-visits?from=2030-08-01&to=2030-08-31")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void cancel_withGerenteRole_returns200() throws Exception {
        String customerId = newCustomerId();
        String funcionarioId = gerenteUserId();
        String response = mockMvc.perform(post("/api/consignment-visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"customerId\":\"" + customerId + "\",\"funcionarioId\":\"" + funcionarioId
                                + "\",\"dataAgendada\":\"2030-09-01\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String visitId = response.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/consignment-visits/" + visitId + "/cancel")
                        .header("Authorization", "Bearer " + gerenteToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELADA")));
    }

    @Test
    void cancel_withVendedorRole_returns403() throws Exception {
        String customerId = newCustomerId();
        String funcionarioId = gerenteUserId();
        String response = mockMvc.perform(post("/api/consignment-visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + gerenteToken())
                        .content("{\"customerId\":\"" + customerId + "\",\"funcionarioId\":\"" + funcionarioId
                                + "\",\"dataAgendada\":\"2030-09-05\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String visitId = response.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/consignment-visits/" + visitId + "/cancel")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isForbidden());
    }
}
