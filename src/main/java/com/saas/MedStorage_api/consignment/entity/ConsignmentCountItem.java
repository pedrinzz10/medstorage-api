package com.saas.MedStorage_api.consignment.entity;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consignment_count_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsignmentCountItem {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "consignment_count_id", nullable = false)
    private ConsignmentCount consignmentCount;

    @ManyToOne
    @JoinColumn(name = "consignment_item_id", nullable = false)
    private ConsignmentItem consignmentItem;

    @Column(name = "quantidade_contada", nullable = false)
    private int quantidadeContada;

    @Column(name = "lote_conferido")
    private String loteConferido;

    @Column(name = "validade_conferida")
    private LocalDate validadeConferida;

    @Column(nullable = false)
    private int divergencia;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
