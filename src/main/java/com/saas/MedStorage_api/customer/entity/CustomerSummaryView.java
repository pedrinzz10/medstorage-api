package com.saas.MedStorage_api.customer.entity;

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
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Immutable
@Subselect("""
        SELECT customer_id, total_pedidos, valor_total_gasto, ultima_compra
        FROM vw_customer_summary
        """)
@Synchronize({"customers", "orders"})
@Getter
@NoArgsConstructor
public class CustomerSummaryView {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "total_pedidos")
    private long totalPedidos;

    @Column(name = "valor_total_gasto")
    private BigDecimal valorTotalGasto;

    @Column(name = "ultima_compra")
    private LocalDateTime ultimaCompra;
}
