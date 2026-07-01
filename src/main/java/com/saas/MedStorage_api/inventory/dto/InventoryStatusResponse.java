package com.saas.MedStorage_api.inventory.dto;

import java.util.UUID;

public record InventoryStatusResponse(
        UUID id,
        String nome,
        String sku,
        int quantidadeAtual,
        int disponivel,
        int reservada,
        Integer estoqueMinimo,
        String statusEstoque,
        java.math.BigDecimal precoBase,
        String unidade
) {
}
