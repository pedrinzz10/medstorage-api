package com.saas.MedStorage_api.consignment.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateVisitRequest(
        @NotNull UUID customerId,
        @NotNull UUID funcionarioId,
        @NotNull @FutureOrPresent LocalDate dataAgendada,
        String observacoes
) {
}
