package com.saas.MedStorage_api.auth.service;

import com.saas.MedStorage_api.auth.dto.LoginRequest;
import com.saas.MedStorage_api.auth.dto.LoginResponse;
import com.saas.MedStorage_api.auth.dto.RegisterRequest;
import com.saas.MedStorage_api.auth.dto.UserSummaryResponse;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.exception.UnauthorizedException;
import com.saas.MedStorage_api.security.JwtProvider;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import com.saas.MedStorage_api.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isAtivo() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Credencial invalida ou conta desativada para user={}", request.email());
            throw new UnauthorizedException("Invalid credentials");
        }

        log.info("Login realizado: user={}", request.email());
        String token = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new LoginResponse(token, UserSummaryResponse.from(user));
    }

    public UserSummaryResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already in use");
        }

        UserRole role;
        try {
            role = UserRole.fromValue(request.role());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role: " + request.role());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nome(request.nome())
                .role(role)
                .ativo(true)
                .telefone(request.telefone())
                .build();

        UserSummaryResponse response = UserSummaryResponse.from(userRepository.save(user));
        log.info("Usuário registrado: email={} role={}", request.email(), request.role());
        return response;
    }

    public LoginResponse refresh(String token) {
        Claims claims = jwtProvider.parseClaims(token);
        UUID userId = UUID.fromString(claims.get("userId", String.class));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!user.isAtivo()) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String newToken = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        log.info("Token renovado: user={} role={}", user.getEmail(), user.getRole());
        return new LoginResponse(newToken, UserSummaryResponse.from(user));
    }
}
