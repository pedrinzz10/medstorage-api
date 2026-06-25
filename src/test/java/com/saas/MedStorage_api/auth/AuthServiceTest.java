package com.saas.MedStorage_api.auth;

import com.saas.MedStorage_api.auth.dto.LoginRequest;
import com.saas.MedStorage_api.auth.dto.LoginResponse;
import com.saas.MedStorage_api.auth.dto.RegisterRequest;
import com.saas.MedStorage_api.auth.dto.UserSummaryResponse;
import com.saas.MedStorage_api.auth.service.AuthService;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import com.saas.MedStorage_api.user.repository.UserRepository;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.exception.UnauthorizedException;
import com.saas.MedStorage_api.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("admin@distribuidor.com")
                .passwordHash("hashed-password")
                .nome("Admin Master")
                .role(UserRole.ADMIN)
                .ativo(true)
                .build();
    }

    @Test
    void login_withValidCredentials_returnsTokenAndUser() {
        when(userRepository.findByEmail("admin@distribuidor.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha123", "hashed-password")).thenReturn(true);
        when(jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole())).thenReturn("fake-jwt-token");

        LoginResponse response = authService.login(new LoginRequest("admin@distribuidor.com", "senha123"));

        assertNotNull(response);
        assertEquals("fake-jwt-token", response.token());
        assertEquals("admin@distribuidor.com", response.user().email());
        assertEquals("admin", response.user().role());
    }

    @Test
    void login_withInvalidPassword_throwsUnauthorizedException() {
        when(userRepository.findByEmail("admin@distribuidor.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senhaerrada", "hashed-password")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("admin@distribuidor.com", "senhaerrada")));
    }

    @Test
    void login_withUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                authService.login(new LoginRequest("notfound@test.com", "password")));
    }

    @Test
    void register_withNewEmail_createsUser() {
        RegisterRequest request = new RegisterRequest(
                "novo@distribuidor.com", "senha123", "Novo Usuario", "vendedor", "11999999999");

        when(userRepository.existsByEmail("novo@distribuidor.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        UserSummaryResponse response = authService.register(request);

        assertNotNull(response.id());
        assertEquals("novo@distribuidor.com", response.email());
        assertEquals("vendedor", response.role());
    }

    @Test
    void register_withExistingEmail_throwsBadRequestException() {
        RegisterRequest request = new RegisterRequest(
                "admin@distribuidor.com", "senha123", "Outro", "admin", null);

        when(userRepository.existsByEmail("admin@distribuidor.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void register_withInvalidRole_throwsBadRequestException() {
        RegisterRequest request = new RegisterRequest(
                "novo@distribuidor.com", "senha123", "Novo", "papel_invalido", null);

        when(userRepository.existsByEmail("novo@distribuidor.com")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }
}
