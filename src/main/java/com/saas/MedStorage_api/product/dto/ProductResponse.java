package com.saas.MedStorage_api.product.dto;

import com.saas.MedStorage_api.product.Product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String nome,
        String descricao,
        String sku,
        BigDecimal precoBase,
        String unidade,
        Integer estoqueMinimo,
        boolean ativo
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getNome(),
                product.getDescricao(),
                product.getSku(),
                product.getPrecoBase(),
                product.getUnidade(),
                product.getEstoqueMinimo(),
                product.isAtivo());
    }
}
