package com.saas.MedStorage_api.batch.dto;

import com.saas.MedStorage_api.batch.entity.ProductBatch;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record BatchResponse(
        UUID id,
        UUID productId,
        String lote,
        LocalDate validade,
        int quantidade,
        long diasParaVencer
) {
    public static BatchResponse from(ProductBatch batch) {
        return new BatchResponse(
                batch.getId(),
                batch.getProduct().getId(),
                batch.getLote(),
                batch.getValidade(),
                batch.getQuantidade(),
                ChronoUnit.DAYS.between(LocalDate.now(), batch.getValidade()));
    }
}
