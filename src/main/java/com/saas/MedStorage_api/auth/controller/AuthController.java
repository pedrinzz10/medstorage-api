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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Autenticação", description = "Login, registro de usuários e gerenciamento de tokens JWT")
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

    @Operation(summary = "Login", description = "Autentica o usuário e retorna um token JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Senha incorreta"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Registrar usuário", description = "Cria um novo usuário (requer papel ADMIN)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado"),
        @ApiResponse(responseCode = "400", description = "E-mail já em uso ou papel inválido"),
        @ApiResponse(responseCode = "403", description = "Sem permissão (somente ADMIN)")
    })
    @PostMapping("/register")
    public ResponseEntity<UserSummaryResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Validar token", description = "Verifica se o token JWT ainda é válido e retorna os dados do usuário")
    @ApiResponse(responseCode = "200", description = "Token válido")
    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.replace(BEARER_PREFIX, "");
        Claims claims = jwtProvider.parseClaims(token);
        return ResponseEntity.ok(new ValidateResponse(true, claims.getSubject(), claims.get("role", String.class)));
    }

    @Operation(summary = "Logout", description = "Endpoint de logout (JWT é stateless — o cliente deve descartar o token)")
    @ApiResponse(responseCode = "200", description = "Logout realizado")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT stateless: nao ha sessao no servidor para invalidar.
        // O cliente descarta o token; revogacao real exigiria uma blacklist.
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Renovar token", description = "Reemite um novo token JWT a partir de um token ainda válido")
    @ApiResponse(responseCode = "200", description = "Novo token emitido")
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
