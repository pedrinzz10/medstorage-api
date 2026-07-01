package com.saas.MedStorage_api.security;

import com.saas.MedStorage_api.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que a configuração de CORS permita qualquer origem localhost (dev)
 * e sempre habilite credenciais — o frontend envia o cookie JWT via
 * credentials:'include', que exige Access-Control-Allow-Credentials: true.
 */
@IntegrationTest
class CorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightFromViteDevOrigin_isAllowedWithCredentials() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void preflightFromAnyLocalhostPort_isAllowed() throws Exception {
        mockMvc.perform(options("/api/orders")
                        .header("Origin", "http://localhost:4173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void preflightFromDisallowedOrigin_isRejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
