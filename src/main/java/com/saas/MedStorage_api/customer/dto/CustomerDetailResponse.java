package com.saas.MedStorage_api.customer.dto;

import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.customer.entity.CustomerSummaryView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record CustomerDetailResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        String cnpj,
        String endereco,
        String contatoPrincipal,
        Map<String, Object> dadosAdicionais,
        long totalPedidos,
        BigDecimal valorTotalGasto,
        LocalDateTime ultimaCompra
) {
    public static CustomerDetailResponse from(Customer customer, CustomerSummaryView summary) {
        return new CustomerDetailResponse(
                customer.getId(),
                customer.getNome(),
                customer.getEmail(),
                customer.getTelefone(),
                customer.getCnpj(),
                customer.getEndereco(),
                customer.getContatoPrincipal(),
                customer.getDadosAdicionais(),
                summary != null ? summary.getTotalPedidos() : 0L,
                summary != null ? summary.getValorTotalGasto() : BigDecimal.ZERO,
                summary != null ? summary.getUltimaCompra() : null);
    }
}
