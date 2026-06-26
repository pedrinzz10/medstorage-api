package com.saas.MedStorage_api.commission.dto;

import com.saas.MedStorage_api.commission.entity.Commission;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CommissionResponse(
        UUID id,
        UUID vendedorId,
        String vendedorNome,
        LocalDate periodoInicio,
        LocalDate periodoFim,
        int totalPedidos,
        BigDecimal valorVendido,
        int quantidadeUnidades,
        BigDecimal taxaComissao,
        BigDecimal valorComissao,
        String status
) {
    public static CommissionResponse from(Commission c) {
        return new CommissionResponse(
                c.getId(),
                c.getVendedor().getId(),
                c.getVendedor().getNome(),
                c.getPeriodoInicio(),
                c.getPeriodoFim(),
                c.getTotalPedidos(),
                c.getValorVendido(),
                c.getQuantidadeUnidades(),
                c.getTaxaComissao(),
                c.getValorComissao(),
                c.getStatus().name());
    }
}
