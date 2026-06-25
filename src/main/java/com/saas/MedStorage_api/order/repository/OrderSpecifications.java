package com.saas.MedStorage_api.order.repository;

import com.saas.MedStorage_api.order.entity.Order;
import com.saas.MedStorage_api.order.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> withFilters(
            OrderStatus status,
            UUID customerId,
            UUID criadoPor,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            BigDecimal valorMin,
            BigDecimal valorMax) {
        return (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            if (status != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("status"), status));
            }
            if (customerId != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("customer").get("id"), customerId));
            }
            if (criadoPor != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("criadoPor").get("id"), criadoPor));
            }
            if (dataInicio != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), dataInicio));
            }
            if (dataFim != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), dataFim));
            }
            if (valorMin != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("valorTotal"), valorMin));
            }
            if (valorMax != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.lessThanOrEqualTo(root.get("valorTotal"), valorMax));
            }

            return predicates;
        };
    }
}
