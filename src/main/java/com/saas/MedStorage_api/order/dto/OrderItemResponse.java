package com.saas.MedStorage_api.order.dto;

import com.saas.MedStorage_api.batch.dto.OrderItemBatchResponse;
import com.saas.MedStorage_api.order.entity.OrderItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productNome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal,
        List<OrderItemBatchResponse> lotes
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getNome(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal(),
                item.getLotes().stream().map(OrderItemBatchResponse::from).toList());
    }
}
