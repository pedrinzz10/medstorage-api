package com.saas.MedStorage_api.consignment.entity;

import com.saas.MedStorage_api.batch.entity.ProductBatch;
import com.saas.MedStorage_api.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consignment_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsignmentItem {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "consignment_id", nullable = false)
    private Consignment consignment;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private ProductBatch batch;

    @Column(name = "quantidade_enviada", nullable = false)
    private int quantidadeEnviada;

    @Column(name = "quantidade_usada", nullable = false)
    @Builder.Default
    private int quantidadeUsada = 0;

    @Column(name = "quantidade_devolvida", nullable = false)
    @Builder.Default
    private int quantidadeDevolvida = 0;

    @Column(name = "preco_unitario", nullable = false)
    private BigDecimal precoUnitario;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public int getSaldoDisponivel() {
        return quantidadeEnviada - quantidadeUsada - quantidadeDevolvida;
    }
}
