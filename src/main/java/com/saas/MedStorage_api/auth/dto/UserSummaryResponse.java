package com.saas.MedStorage_api.auth.dto;

import com.saas.MedStorage_api.user.User;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String email,
        String nome,
        String role
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getEmail(), user.getNome(), user.getRole().getValue());
    }
}
