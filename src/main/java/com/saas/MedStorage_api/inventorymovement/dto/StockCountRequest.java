package com.saas.MedStorage_api.inventorymovement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockCountRequest(
        @NotNull UUID productId,
        @Min(0) int quantidadeContada,
        @NotBlank String observacao
) {
}
