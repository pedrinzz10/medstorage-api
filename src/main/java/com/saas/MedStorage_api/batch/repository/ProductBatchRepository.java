package com.saas.MedStorage_api.batch.repository;

import com.saas.MedStorage_api.batch.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, UUID> {

    List<ProductBatch> findByProductIdOrderByValidadeAsc(UUID productId);

    Optional<ProductBatch> findByProductIdAndLote(UUID productId, String lote);
}
