package com.saas.MedStorage_api.seller.dto;

import com.saas.MedStorage_api.seller.entity.SellerPerformanceView;
import com.saas.MedStorage_api.user.entity.User;

import java.math.BigDecimal;
import java.util.UUID;

public record SellerPerformanceResponse(
        UUID vendedorId,
        String vendedorNome,
        String vendedorEmail,
        int totalPedidos,
        BigDecimal valorVendido,
        int quantidadeUnidades
) {
    public static SellerPerformanceResponse from(SellerPerformanceView view) {
        return new SellerPerformanceResponse(
                view.getVendedorId(),
                view.getVendedorNome(),
                view.getVendedorEmail(),
                view.getTotalPedidos(),
                view.getValorVendido(),
                view.getQuantidadeUnidades());
    }

    public static SellerPerformanceResponse empty(User user) {
        return new SellerPerformanceResponse(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                0,
                BigDecimal.ZERO,
                0);
    }
}
