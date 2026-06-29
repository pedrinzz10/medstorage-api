package com.saas.MedStorage_api.user;

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
class UserControllerIntegrationTest {

    private static final char[] ADMIN_SECRET = {'A', 'd', 'm', 'i', 'n', '1', '2', '3', '!'};
    private static final char[] VENDEDOR_SECRET = {'V', 'e', 'n', 'd', 'e', 'd', 'o', 'r', '1', '2', '3', '!'};

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

    @Test
    void findAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findAll_withVendedorToken_returns403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + vendedorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAll_withAdminToken_returns200WithUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void findById_withAdminToken_returns200() throws Exception {
        String listJson = mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse().getContentAsString();
        String userId = listJson.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.role").exists());
    }

    @Test
    void findById_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/users/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_withAdminToken_returns200WithUpdatedFields() throws Exception {
        String token = adminToken();
        String listJson = mockMvc.perform(get("/api/users?size=20")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String vendedorId = listJson.split("\"email\":\"vendedor1@distribuidor.com\"")[0]
                .replaceAll(".*\"id\":\"", "").replaceAll("\".*", "");

        mockMvc.perform(patch("/api/users/" + vendedorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"nome\":\"João Atualizado\",\"telefone\":\"11900001111\",\"role\":\"vendedor\",\"ativo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João Atualizado"))
                .andExpect(jsonPath("$.telefone").value("11900001111"));
    }

    @Test
    void update_withInvalidRole_returns400() throws Exception {
        String token = adminToken();
        String listJson = mockMvc.perform(get("/api/users?size=20")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String userId = listJson.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"nome\":\"Nome\",\"role\":\"diretor\",\"ativo\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_withVendedorToken_returns403() throws Exception {
        mockMvc.perform(patch("/api/users/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendedorToken())
                        .content("{\"nome\":\"Hack\",\"role\":\"admin\",\"ativo\":true}"))
                .andExpect(status().isForbidden());
    }
}
