package com.saas.MedStorage_api.auth.controller;

import com.saas.MedStorage_api.auth.dto.LoginRequest;
import com.saas.MedStorage_api.auth.dto.LoginResponse;
import com.saas.MedStorage_api.auth.dto.RegisterRequest;
import com.saas.MedStorage_api.auth.dto.UserSummaryResponse;
import com.saas.MedStorage_api.auth.dto.ValidateResponse;
import com.saas.MedStorage_api.auth.service.AuthService;
import com.saas.MedStorage_api.auth.service.LoginRateLimiter;
import com.saas.MedStorage_api.exception.TooManyRequestsException;
import com.saas.MedStorage_api.security.JwtProvider;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "Autenticação", description = "Login, registro de usuários e gerenciamento de tokens JWT")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String JWT_COOKIE = "jwt";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final LoginRateLimiter rateLimiter;
    private final long jwtExpirationMs;
    private final boolean cookieSecure;
    private final String trustedProxiesConfig;

    public AuthController(
            AuthService authService,
            JwtProvider jwtProvider,
            LoginRateLimiter rateLimiter,
            @Value("${jwt.expiration-ms:86400000}") long jwtExpirationMs,
            @Value("${app.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.trusted-proxies:}") String trustedProxiesConfig) {
        this.authService = authService;
        this.jwtProvider = jwtProvider;
        this.rateLimiter = rateLimiter;
        this.jwtExpirationMs = jwtExpirationMs;
        this.cookieSecure = cookieSecure;
        this.trustedProxiesConfig = trustedProxiesConfig;
    }

    @Operation(summary = "Login", description = "Autentica o usuário e emite o token JWT via cookie HttpOnly. Limitado a 5 tentativas por minuto por IP.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Senha incorreta ou conta desativada"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "429", description = "Muitas tentativas — aguarde 1 minuto")
    })
    @PostMapping("/login")
    public ResponseEntity<UserSummaryResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String clientIp = resolveClientIp(httpRequest);
        if (!rateLimiter.isAllowed(clientIp)) {
            throw new TooManyRequestsException("Too many login attempts. Please try again in a minute.");
        }
        LoginResponse result = authService.login(request);
        setJwtCookie(httpResponse, result.token());
        return ResponseEntity.ok(result.user());
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

    @Operation(summary = "Validar token", description = "Verifica se o token JWT ainda é válido — lê do cookie ou do header Authorization")
    @ApiResponse(responseCode = "200", description = "Token válido")
    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest request) {
        String token = extractToken(authorizationHeader, request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Claims claims = jwtProvider.parseClaims(token);
        return ResponseEntity.ok(new ValidateResponse(true, claims.getSubject(), claims.get("role", String.class)));
    }

    @Operation(summary = "Logout", description = "Invalida o cookie JWT no cliente")
    @ApiResponse(responseCode = "200", description = "Logout realizado")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        clearJwtCookie(httpResponse);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Renovar token", description = "Reemite um novo token JWT consultando o banco — reflete papel e status atuais do usuário")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Novo token emitido"),
        @ApiResponse(responseCode = "401", description = "Token inválido ou conta desativada")
    })
    @PostMapping("/refresh")
    public ResponseEntity<UserSummaryResponse> refresh(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest request,
            HttpServletResponse httpResponse) {
        String token = extractToken(authorizationHeader, request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LoginResponse result = authService.refresh(token);
        setJwtCookie(httpResponse, result.token());
        return ResponseEntity.ok(result.user());
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(jwtExpirationMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractToken(String authorizationHeader, HttpServletRequest request) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (JWT_COOKIE.equals(c.getName())) return c.getValue();
            }
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxiesConfig.isBlank()) {
            Set<String> trusted = Arrays.stream(trustedProxiesConfig.split(","))
                    .map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            if (trusted.contains(remoteAddr)) {
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].strip();
                }
            }
        }
        return remoteAddr;
    }
}
