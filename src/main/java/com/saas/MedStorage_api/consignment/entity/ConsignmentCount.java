package com.saas.MedStorage_api.consignment.entity;

import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "consignment_counts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsignmentCount {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "visit_id")
    private ConsignmentVisit visit;

    @ManyToOne
    @JoinColumn(name = "funcionario_id", nullable = false)
    private User funcionario;

    @Column(name = "data_contagem", nullable = false)
    private LocalDate dataContagem;

    @OneToMany(mappedBy = "consignmentCount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConsignmentCountItem> items = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public void addItem(ConsignmentCountItem item) {
        items.add(item);
        item.setConsignmentCount(this);
    }
}
