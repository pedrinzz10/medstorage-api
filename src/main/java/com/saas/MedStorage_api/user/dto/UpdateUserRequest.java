package com.saas.MedStorage_api.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank String nome,
        String telefone,
        @NotBlank String role,
        boolean ativo
) {}
