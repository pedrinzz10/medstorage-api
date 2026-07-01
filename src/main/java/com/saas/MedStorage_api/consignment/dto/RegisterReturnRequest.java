package com.saas.MedStorage_api.consignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterReturnRequest(
        @NotNull @Positive Integer quantidade
) {
}
