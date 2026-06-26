package com.saas.MedStorage_api.returns.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReturnItemRequest(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantidade
) {
}
