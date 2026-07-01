package com.saas.MedStorage_api.batch.dto;

import com.saas.MedStorage_api.batch.entity.OrderItemBatch;

import java.time.LocalDate;

public record OrderItemBatchResponse(
        String lote,
        LocalDate validade,
        int quantidadeConsumida
) {
    public static OrderItemBatchResponse from(OrderItemBatch orderItemBatch) {
        return new OrderItemBatchResponse(
                orderItemBatch.getBatch().getLote(),
                orderItemBatch.getBatch().getValidade(),
                orderItemBatch.getQuantidadeConsumida());
    }
}
