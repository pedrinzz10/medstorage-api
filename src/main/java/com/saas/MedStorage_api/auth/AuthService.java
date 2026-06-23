package com.saas.MedStorage_api.auth;

import com.saas.MedStorage_api.auth.dto.LoginRequest;
import com.saas.MedStorage_api.auth.dto.LoginResponse;
import com.saas.MedStorage_api.auth.dto.RegisterRequest;
import com.saas.MedStorage_api.auth.dto.UserSummaryResponse;
import com.saas.MedStorage_api.domain.user.User;
import com.saas.MedStorage_api.domain.user.UserRepository;
import com.saas.MedStorage_api.domain.user.UserRole;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.exception.UnauthorizedException;
import com.saas.MedStorage_api.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

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

        return UserSummaryResponse.from(userRepository.save(user));
    }
}
