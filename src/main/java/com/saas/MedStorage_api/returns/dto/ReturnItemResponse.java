package com.saas.MedStorage_api.returns.dto;

import com.saas.MedStorage_api.returns.entity.ReturnItem;

import java.math.BigDecimal;
import java.util.UUID;

public record ReturnItemResponse(
        UUID id,
        UUID productId,
        String productNome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
    public static ReturnItemResponse from(ReturnItem item) {
        return new ReturnItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getNome(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal());
    }
}
