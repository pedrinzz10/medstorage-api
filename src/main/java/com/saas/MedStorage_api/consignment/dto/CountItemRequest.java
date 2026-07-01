package com.saas.MedStorage_api.consignment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CountItemRequest(
        @NotNull UUID consignmentItemId,
        @NotNull @Min(0) Integer quantidadeContada,
        String loteConferido,
        LocalDate validadeConferida
) {
}
