package com.saas.MedStorage_api.returns.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateReturnRequest(
        @NotNull UUID orderId,
        @NotEmpty @Valid List<ReturnItemRequest> items,
        String motivo
) {
}
