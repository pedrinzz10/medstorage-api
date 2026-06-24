package com.saas.MedStorage_api.domain.inventorymovement;

import com.saas.MedStorage_api.domain.product.Product;
import com.saas.MedStorage_api.domain.user.User;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovement {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType tipo;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false)
    private String motivo;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(name = "referencia_tipo")
    private String referenciaTipo;

    @ManyToOne
    @JoinColumn(name = "criado_por")
    private User criadoPor;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
