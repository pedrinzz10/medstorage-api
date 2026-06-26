package com.saas.MedStorage_api.seller.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "vw_seller_performance_current_month")
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
