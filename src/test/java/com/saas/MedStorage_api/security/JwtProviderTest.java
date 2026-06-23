package com.saas.MedStorage_api.security;

import com.saas.MedStorage_api.domain.user.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            "test-secret-key-with-at-least-32-characters-long",
            86400000L);

    @Test
    void generateToken_thenParseClaims_returnsOriginalData() {
        UUID userId = UUID.randomUUID();
        String token = jwtProvider.generateToken(userId, "admin@distribuidor.com", UserRole.ADMIN);

        Claims claims = jwtProvider.parseClaims(token);

        assertEquals("admin@distribuidor.com", claims.getSubject());
        assertEquals(userId.toString(), claims.get("userId", String.class));
        assertEquals("admin", claims.get("role", String.class));
    }

    @Test
    void isValid_withGeneratedToken_returnsTrue() {
        String token = jwtProvider.generateToken(UUID.randomUUID(), "user@test.com", UserRole.VENDEDOR);

        assertTrue(jwtProvider.isValid(token));
    }

    @Test
    void isValid_withMalformedToken_returnsFalse() {
        assertFalse(jwtProvider.isValid("not-a-valid-token"));
    }

    @Test
    void parseClaims_withMalformedToken_throwsInvalidTokenException() {
        assertThrows(InvalidTokenException.class, () -> jwtProvider.parseClaims("not-a-valid-token"));
    }

    @Test
    void parseClaims_withTokenSignedByDifferentKey_throwsInvalidTokenException() {
        JwtProvider otherProvider = new JwtProvider(
                "another-secret-key-with-at-least-32-characters",
                86400000L);
        String token = otherProvider.generateToken(UUID.randomUUID(), "user@test.com", UserRole.VENDEDOR);

        assertThrows(InvalidTokenException.class, () -> jwtProvider.parseClaims(token));
    }

    @Test
    void parseClaims_withExpiredToken_throwsInvalidTokenException() {
        JwtProvider shortLivedProvider = new JwtProvider(
                "test-secret-key-with-at-least-32-characters-long",
                -1000L);
        String token = shortLivedProvider.generateToken(UUID.randomUUID(), "user@test.com", UserRole.VENDEDOR);

        assertThrows(InvalidTokenException.class, () -> jwtProvider.parseClaims(token));
    }
}
