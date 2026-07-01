package com.saas.MedStorage_api.batch.service;

import com.saas.MedStorage_api.batch.dto.BatchOrderTraceResponse;
import com.saas.MedStorage_api.batch.dto.BatchResponse;
import com.saas.MedStorage_api.batch.repository.OrderItemBatchRepository;
import com.saas.MedStorage_api.batch.repository.ProductBatchRepository;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BatchService {

    private final ProductBatchRepository productBatchRepository;
    private final ProductRepository productRepository;
    private final OrderItemBatchRepository orderItemBatchRepository;

    public BatchService(
            ProductBatchRepository productBatchRepository,
            ProductRepository productRepository,
            OrderItemBatchRepository orderItemBatchRepository) {
        this.productBatchRepository = productBatchRepository;
        this.productRepository = productRepository;
        this.orderItemBatchRepository = orderItemBatchRepository;
    }

    public List<BatchResponse> findByProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }
        return productBatchRepository.findByProductIdOrderByValidadeAsc(productId).stream()
                .map(BatchResponse::from)
                .toList();
    }

    public List<BatchOrderTraceResponse> findOrdersForBatch(UUID batchId) {
        if (!productBatchRepository.existsById(batchId)) {
            throw new ResourceNotFoundException("Batch not found: " + batchId);
        }
        return orderItemBatchRepository.findByBatchId(batchId).stream()
                .map(BatchOrderTraceResponse::from)
                .toList();
    }
}
