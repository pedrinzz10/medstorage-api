package com.saas.MedStorage_api.commission.entity;

import com.saas.MedStorage_api.commission.enums.CommissionStatus;
import com.saas.MedStorage_api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "commissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commission {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "vendedor_id", nullable = false)
    private User vendedor;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodoFim;

    @Column(name = "total_pedidos", nullable = false)
    @Builder.Default
    private int totalPedidos = 0;

    @Column(name = "valor_vendido", nullable = false)
    @Builder.Default
    private BigDecimal valorVendido = BigDecimal.ZERO;

    @Column(name = "quantidade_unidades", nullable = false)
    @Builder.Default
    private int quantidadeUnidades = 0;

    @Column(name = "taxa_comissao", nullable = false)
    @Builder.Default
    private BigDecimal taxaComissao = BigDecimal.ZERO;

    @Generated(event = EventType.INSERT)
    @Column(name = "valor_comissao", insertable = false, updatable = false)
    private BigDecimal valorComissao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CommissionStatus status = CommissionStatus.PENDENTE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
