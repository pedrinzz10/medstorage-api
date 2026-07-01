package com.saas.MedStorage_api.inventorymovement.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record StockAdjustmentRequest(
        @NotNull UUID productId,
        @Min(1) int quantidade,
        @NotBlank String motivo,
        @NotBlank String lote,
        @NotNull @FutureOrPresent LocalDate validade
) {
}
