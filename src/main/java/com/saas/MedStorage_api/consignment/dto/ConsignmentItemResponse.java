package com.saas.MedStorage_api.consignment.dto;

import com.saas.MedStorage_api.consignment.entity.ConsignmentItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ConsignmentItemResponse(
        UUID id,
        UUID productId,
        String productNome,
        String lote,
        LocalDate validade,
        int quantidadeEnviada,
        int quantidadeUsada,
        int quantidadeDevolvida,
        int saldoDisponivel,
        BigDecimal precoUnitario
) {
    public static ConsignmentItemResponse from(ConsignmentItem item) {
        return new ConsignmentItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getNome(),
                item.getBatch() != null ? item.getBatch().getLote() : null,
                item.getBatch() != null ? item.getBatch().getValidade() : null,
                item.getQuantidadeEnviada(),
                item.getQuantidadeUsada(),
                item.getQuantidadeDevolvida(),
                item.getSaldoDisponivel(),
                item.getPrecoUnitario());
    }
}
