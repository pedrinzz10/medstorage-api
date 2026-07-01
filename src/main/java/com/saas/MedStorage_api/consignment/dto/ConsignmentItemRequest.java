package com.saas.MedStorage_api.consignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ConsignmentItemRequest(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantidade
) {
}
