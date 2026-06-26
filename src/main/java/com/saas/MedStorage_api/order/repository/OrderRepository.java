package com.saas.MedStorage_api.order.repository;

import com.saas.MedStorage_api.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @Query(value = "select nextval('order_numero_seq')", nativeQuery = true)
    long nextNumeroPedidoSequence();
}
