package com.saas.MedStorage_api.consignment.entity;

import com.saas.MedStorage_api.consignment.enums.ConsignmentStatus;
import com.saas.MedStorage_api.customer.entity.Customer;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "consignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consignment {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConsignmentStatus status = ConsignmentStatus.ATIVO;

    private String observacoes;

    @ManyToOne
    @JoinColumn(name = "criado_por")
    private User criadoPor;

    @OneToMany(mappedBy = "consignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConsignmentItem> items = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public void addItem(ConsignmentItem item) {
        items.add(item);
        item.setConsignment(this);
    }
}
