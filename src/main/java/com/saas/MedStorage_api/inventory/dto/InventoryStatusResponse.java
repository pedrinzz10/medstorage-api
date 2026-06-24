package com.saas.MedStorage_api.inventory.dto;

import java.util.UUID;

public record InventoryStatusResponse(
        UUID id,
        String nome,
        String sku,
        int quantidadeAtual,
        Integer estoqueMinimo,
        String statusEstoque
) {
}
