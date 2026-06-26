package com.saas.MedStorage_api.returns.dto;

import com.saas.MedStorage_api.returns.entity.Return;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReturnResponse(
        UUID id,
        String numeroRetorno,
        UUID orderId,
        String numeroPedido,
        UUID processadoPor,
        String status,
        String motivo,
        LocalDateTime dataSolicitacao,
        LocalDateTime dataProcessamento,
        List<ReturnItemResponse> items
) {
    public static ReturnResponse from(Return ret) {
        return new ReturnResponse(
                ret.getId(),
                ret.getNumeroRetorno(),
                ret.getOrder().getId(),
                ret.getOrder().getNumeroPedido(),
                ret.getProcessadoPor() != null ? ret.getProcessadoPor().getId() : null,
                ret.getStatus().name(),
                ret.getMotivo(),
                ret.getDataSolicitacao(),
                ret.getDataProcessamento(),
                ret.getItems().stream().map(ReturnItemResponse::from).toList());
    }
}
