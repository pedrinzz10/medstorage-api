package com.saas.MedStorage_api.inventorymovement.dto;

import com.saas.MedStorage_api.inventorymovement.entity.InventoryMovement;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryMovementResponse(
        UUID id,
        UUID productId,
        String productNome,
        String tipo,
        int quantidade,
        String motivo,
        UUID referenciaId,
        String referenciaTipo,
        String criadoPorEmail,
        LocalDateTime createdAt
) {
    public static InventoryMovementResponse from(InventoryMovement m) {
        return new InventoryMovementResponse(
                m.getId(),
                m.getProduct().getId(),
                m.getProduct().getNome(),
                m.getTipo().name(),
                m.getQuantidade(),
                m.getMotivo(),
                m.getReferenciaId(),
                m.getReferenciaTipo(),
                m.getCriadoPor() != null ? m.getCriadoPor().getEmail() : null,
                m.getCreatedAt());
    }
}
