package com.saas.MedStorage_api.consignment.dto;

import com.saas.MedStorage_api.consignment.entity.ConsignmentUsage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ConsignmentUsageResponse(
        UUID id,
        UUID consignmentItemId,
        int quantidade,
        BigDecimal valorFaturado,
        LocalDate dataUso
) {
    public static ConsignmentUsageResponse from(ConsignmentUsage usage) {
        return new ConsignmentUsageResponse(
                usage.getId(),
                usage.getConsignmentItem().getId(),
                usage.getQuantidade(),
                usage.getValorFaturado(),
                usage.getDataUso());
    }
}
