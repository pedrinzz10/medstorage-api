package com.saas.MedStorage_api.security;

import com.saas.MedStorage_api.user.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokenScenarios")
    void parseClaims_withInvalidToken_throwsInvalidTokenException(String scenario, Supplier<String> tokenSupplier) {
        assertThrows(InvalidTokenException.class, () -> jwtProvider.parseClaims(tokenSupplier.get()));
    }

    private static Stream<Arguments> invalidTokenScenarios() {
        return Stream.of(
                Arguments.of("token malformado (string arbitraria)",
                        (Supplier<String>) () -> "not-a-valid-token"),
                Arguments.of("token assinado com outra chave",
                        (Supplier<String>) () -> new JwtProvider(
                                "another-secret-key-with-at-least-32-characters", 86400000L)
                                .generateToken(UUID.randomUUID(), "user@test.com", UserRole.VENDEDOR)),
                Arguments.of("token expirado",
                        (Supplier<String>) () -> new JwtProvider(
                                "test-secret-key-with-at-least-32-characters-long", -1000L)
                                .generateToken(UUID.randomUUID(), "user@test.com", UserRole.VENDEDOR))
        );
    }
}
