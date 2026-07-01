package com.saas.MedStorage_api.consignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegisterCountRequest(
        @NotNull UUID customerId,
        UUID visitId,
        @NotNull LocalDate dataContagem,
        @NotEmpty @Valid List<CountItemRequest> items
) {
}
