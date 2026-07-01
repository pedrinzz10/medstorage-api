package com.saas.MedStorage_api.consignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record RegisterUsageRequest(
        @NotNull @Positive Integer quantidade,
        @NotNull LocalDate dataUso
) {
}
