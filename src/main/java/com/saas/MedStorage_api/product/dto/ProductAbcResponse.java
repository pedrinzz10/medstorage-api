package com.saas.MedStorage_api.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductAbcResponse(
        UUID productId,
        String nome,
        String sku,
        BigDecimal valorVendido,
        int quantidadeVendida,
        BigDecimal percentualAcumulado,
        String classe
) {
}
