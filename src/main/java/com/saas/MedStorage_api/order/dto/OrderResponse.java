package com.saas.MedStorage_api.order.dto;

import com.saas.MedStorage_api.order.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String numeroPedido,
        UUID customerId,
        String customerNome,
        UUID criadoPor,
        String status,
        BigDecimal valorTotal,
        BigDecimal descontoAplicado,
        String tipoDesconto,
        String notas,
        LocalDateTime dataConfirmado,
        LocalDateTime dataSeparado,
        LocalDateTime dataPronte,
        LocalDateTime dataFinalizado,
        UUID finalizadoPor,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getNumeroPedido(),
                order.getCustomer().getId(),
                order.getCustomer().getNome(),
                order.getCriadoPor().getId(),
                order.getStatus().name(),
                order.getValorTotal(),
                order.getDescontoAplicado(),
                order.getTipoDesconto(),
                order.getNotas(),
                order.getDataConfirmado(),
                order.getDataSeparado(),
                order.getDataPronte(),
                order.getDataFinalizado(),
                order.getFinalizadoPor() != null ? order.getFinalizadoPor().getId() : null,
                order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
