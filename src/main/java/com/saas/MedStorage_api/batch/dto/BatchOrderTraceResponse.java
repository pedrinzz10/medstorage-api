package com.saas.MedStorage_api.batch.dto;

import com.saas.MedStorage_api.batch.entity.OrderItemBatch;
import com.saas.MedStorage_api.order.entity.Order;

import java.time.LocalDateTime;

public record BatchOrderTraceResponse(
        String numeroPedido,
        String customerNome,
        String status,
        int quantidadeConsumida,
        LocalDateTime dataConsumo
) {
    public static BatchOrderTraceResponse from(OrderItemBatch orderItemBatch) {
        Order order = orderItemBatch.getOrderItem().getOrder();
        return new BatchOrderTraceResponse(
                order.getNumeroPedido(),
                order.getCustomer().getNome(),
                order.getStatus().name(),
                orderItemBatch.getQuantidadeConsumida(),
                orderItemBatch.getCreatedAt());
    }
}
