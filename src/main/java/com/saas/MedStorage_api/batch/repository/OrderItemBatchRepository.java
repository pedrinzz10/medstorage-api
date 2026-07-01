package com.saas.MedStorage_api.batch.repository;

import com.saas.MedStorage_api.batch.entity.OrderItemBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemBatchRepository extends JpaRepository<OrderItemBatch, UUID> {

    List<OrderItemBatch> findByOrderItemId(UUID orderItemId);

    List<OrderItemBatch> findByBatchId(UUID batchId);
}
