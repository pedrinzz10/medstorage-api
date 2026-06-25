package com.saas.MedStorage_api.auth.controller;

import com.saas.MedStorage_api.auth.dto.LoginRequest;
import com.saas.MedStorage_api.auth.dto.LoginResponse;
import com.saas.MedStorage_api.auth.dto.RegisterRequest;
import com.saas.MedStorage_api.auth.dto.UserSummaryResponse;
import com.saas.MedStorage_api.auth.dto.ValidateResponse;
import com.saas.MedStorage_api.auth.service.AuthService;
import com.saas.MedStorage_api.security.JwtProvider;
import com.saas.MedStorage_api.user.enums.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    public AuthController(AuthService authService, JwtProvider jwtProvider) {
        this.authService = authService;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<UserSummaryResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.replace(BEARER_PREFIX, "");
        Claims claims = jwtProvider.parseClaims(token);
        return ResponseEntity.ok(new ValidateResponse(true, claims.getSubject(), claims.get("role", String.class)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT stateless: nao ha sessao no servidor para invalidar.
        // O cliente descarta o token; revogacao real exigiria uma blacklist.
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.replace(BEARER_PREFIX, "");
        Claims claims = jwtProvider.parseClaims(token);
        String newToken = jwtProvider.generateToken(
                UUID.fromString(claims.get("userId", String.class)),
                claims.getSubject(),
                UserRole.fromValue(claims.get("role", String.class)));
        return ResponseEntity.ok(new LoginResponse(newToken, null));
    }
}
