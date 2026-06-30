package com.saas.MedStorage_api.order.entity;

import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.order.enums.OrderStatus;
import com.saas.MedStorage_api.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "numero_pedido", updatable = false)
    private String numeroPedido;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "criado_por", nullable = false)
    private User criadoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.CRIADO;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    @Column(name = "desconto_aplicado")
    @Builder.Default
    private BigDecimal descontoAplicado = BigDecimal.ZERO;

    @Column(name = "tipo_desconto")
    private String tipoDesconto;

    private String notas;

    @Column(name = "data_confirmado")
    private LocalDateTime dataConfirmado;

    @Column(name = "data_separado")
    private LocalDateTime dataSeparado;

    @Column(name = "data_pronto")
    private LocalDateTime dataPronte;

    @Column(name = "data_finalizado")
    private LocalDateTime dataFinalizado;

    @ManyToOne
    @JoinColumn(name = "finalizado_por")
    private User finalizadoPor;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
