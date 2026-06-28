package com.saas.MedStorage_api.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String nome,
        String descricao,
        @NotBlank String sku,
        @NotNull @DecimalMin("0.01") BigDecimal precoBase,
        String unidade,
        Integer estoqueMinimo,
        boolean ativo
) {}
