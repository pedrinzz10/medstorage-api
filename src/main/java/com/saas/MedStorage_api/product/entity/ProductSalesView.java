package com.saas.MedStorage_api.product.entity;

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
        SELECT product_id, nome, sku, valor_vendido, quantidade_vendida
        FROM vw_product_sales
        """)
@Synchronize({"products", "orders", "order_items"})
@Getter
@NoArgsConstructor
public class ProductSalesView {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "product_id")
    private UUID productId;

    private String nome;

    private String sku;

    @Column(name = "valor_vendido")
    private BigDecimal valorVendido;

    @Column(name = "quantidade_vendida")
    private int quantidadeVendida;
}
