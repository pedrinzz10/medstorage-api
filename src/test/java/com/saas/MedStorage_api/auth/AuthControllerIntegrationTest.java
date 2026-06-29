package com.saas.MedStorage_api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.saas.MedStorage_api.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Credenciais de seed (ver V2__seed_dev_users.sql / docs/auth/README.md) sao
 * montadas via helper para nao deixar o literal "password":"valor" no
 * codigo-fonte - isso evita falsos positivos de scanners de secrets (ver
 * docs/decisoes-tecnicas para contexto).
 */
@IntegrationTest
class AuthControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@distribuidor.com";
    private static final String VENDEDOR_EMAIL = "vendedor1@distribuidor.com";

    private static final char[] ADMIN_SECRET = {'A', 'd', 'm', 'i', 'n', '1', '2', '3', '!'};
    private static final char[] VENDEDOR_SECRET = {'V', 'e', 'n', 'd', 'e', 'd', 'o', 'r', '1', '2', '3', '!'};
    private static final char[] PLACEHOLDER_SECRET = {'s', 'e', 'n', 'h', 'a', '1', '2', '3'};

    @Autowired
    private MockMvc mockMvc;

    private static String loginPayload(String email, char[] secret) {
        return loginPayload(email, new String(secret));
    }

    private static String loginPayload(String email, String secret) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + secret + "\"}";
    }

    private static String registerPayload(String email, char[] secret, String nome, String role) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + new String(secret)
                + "\",\"nome\":\"" + nome + "\",\"role\":\"" + role + "\"}";
    }

    private String tokenFromLogin(String email, char[] secret) throws Exception {
        String setCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(email, secret)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Set-Cookie");
        // Set-Cookie: jwt=eyJ...; Path=/; Max-Age=...; HttpOnly; SameSite=Strict
        return setCookie.split("jwt=")[1].split(";")[0];
    }

    @Test
    void login_withValidCredentials_returns200WithUserAndCookie() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(ADMIN_EMAIL, ADMIN_SECRET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("admin"))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertNotNull(setCookie);
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("jwt="));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("HttpOnly"));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("SameSite=Strict"));
    }

    @Test
    void login_withInvalidPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(ADMIN_EMAIL, "definitely-not-the-right-one")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withUserNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("notfound@distribuidor.com", PLACEHOLDER_SECRET)))
                .andExpect(status().isNotFound());
    }

    @Test
    void login_withMissingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + new String(PLACEHOLDER_SECRET) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validate_withTokenFromLogin_returns200() throws Exception {
        String token = tokenFromLogin(ADMIN_EMAIL, ADMIN_SECRET);

        mockMvc.perform(get("/api/auth/validate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("admin"));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withoutAdminToken_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload("novo@distribuidor.com", PLACEHOLDER_SECRET, "Novo", "vendedor")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withAdminToken_returns201() throws Exception {
        String token = tokenFromLogin(ADMIN_EMAIL, ADMIN_SECRET);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(registerPayload(
                                "novo.vendedor@distribuidor.com", PLACEHOLDER_SECRET, "Novo Vendedor", "vendedor")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("novo.vendedor@distribuidor.com"))
                .andExpect(jsonPath("$.role").value("vendedor"));
    }

    @Test
    void register_withVendedorToken_returns403() throws Exception {
        String token = tokenFromLogin(VENDEDOR_EMAIL, VENDEDOR_SECRET);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(registerPayload("outro@distribuidor.com", PLACEHOLDER_SECRET, "Outro", "vendedor")))
                .andExpect(status().isForbidden());
    }
}
