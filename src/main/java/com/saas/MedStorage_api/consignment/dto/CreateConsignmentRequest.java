package com.saas.MedStorage_api.consignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateConsignmentRequest(
        @NotNull UUID customerId,
        @NotEmpty @Valid List<ConsignmentItemRequest> items,
        String observacoes
) {
}
