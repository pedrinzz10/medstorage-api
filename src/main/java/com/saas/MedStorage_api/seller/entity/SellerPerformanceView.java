package com.saas.MedStorage_api.seller.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Immutable
@Subselect("""
        SELECT vendedor_id, vendedor_nome, vendedor_email, total_pedidos, valor_vendido, quantidade_unidades
        FROM vw_seller_performance_current_month
        """)
@Synchronize({"users", "orders", "order_items"})
@Getter
@NoArgsConstructor
public class SellerPerformanceView {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "vendedor_id")
    private UUID vendedorId;

    @Column(name = "vendedor_nome")
    private String vendedorNome;

    @Column(name = "vendedor_email")
    private String vendedorEmail;

    @Column(name = "total_pedidos")
    private int totalPedidos;

    @Column(name = "valor_vendido")
    private BigDecimal valorVendido;

    @Column(name = "quantidade_unidades")
    private int quantidadeUnidades;
}
