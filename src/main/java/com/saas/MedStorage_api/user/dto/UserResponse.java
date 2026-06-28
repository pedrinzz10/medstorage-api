package com.saas.MedStorage_api.user.dto;

import com.saas.MedStorage_api.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String nome,
        String role,
        boolean ativo,
        String telefone,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNome(),
                user.getRole().getValue(),
                user.isAtivo(),
                user.getTelefone(),
                user.getCreatedAt());
    }
}
