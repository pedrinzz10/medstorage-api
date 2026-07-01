package com.saas.MedStorage_api.batch;

import com.saas.MedStorage_api.batch.dto.BatchOrderTraceResponse;
import com.saas.MedStorage_api.batch.dto.BatchResponse;
import com.saas.MedStorage_api.batch.entity.OrderItemBatch;
import com.saas.MedStorage_api.batch.entity.ProductBatch;
import com.saas.MedStorage_api.batch.repository.OrderItemBatchRepository;
import com.saas.MedStorage_api.batch.repository.ProductBatchRepository;
import com.saas.MedStorage_api.batch.service.BatchService;
import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.order.entity.Order;
import com.saas.MedStorage_api.order.entity.OrderItem;
import com.saas.MedStorage_api.order.enums.OrderStatus;
import com.saas.MedStorage_api.product.entity.Product;
import com.saas.MedStorage_api.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private ProductBatchRepository productBatchRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderItemBatchRepository orderItemBatchRepository;

    @InjectMocks
    private BatchService batchService;

    @Test
    void findByProduct_withUnknownProduct_throwsResourceNotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.existsById(productId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> batchService.findByProduct(productId));
    }

    @Test
    void findByProduct_returnsBatchesOrderedByValidadeAsc() {
        UUID productId = UUID.randomUUID();
        Product produto = Product.builder().id(productId).nome("Luva").build();
        ProductBatch antigo = ProductBatch.builder().id(UUID.randomUUID()).product(produto)
                .lote("L1").validade(LocalDate.now().plusMonths(1)).quantidade(10).build();
        ProductBatch novo = ProductBatch.builder().id(UUID.randomUUID()).product(produto)
                .lote("L2").validade(LocalDate.now().plusMonths(6)).quantidade(5).build();

        when(productRepository.existsById(productId)).thenReturn(true);
        when(productBatchRepository.findByProductIdOrderByValidadeAsc(productId)).thenReturn(List.of(antigo, novo));

        List<BatchResponse> result = batchService.findByProduct(productId);

        assertEquals(2, result.size());
        assertEquals("L1", result.get(0).lote());
        assertEquals("L2", result.get(1).lote());
    }

    @Test
    void findOrdersForBatch_withUnknownBatch_throwsResourceNotFound() {
        UUID batchId = UUID.randomUUID();
        when(productBatchRepository.existsById(batchId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> batchService.findOrdersForBatch(batchId));
    }

    @Test
    void findOrdersForBatch_returnsOrdersThatConsumedTheBatch() {
        UUID batchId = UUID.randomUUID();
        Product produto = Product.builder().id(UUID.randomUUID()).nome("Luva").build();
        ProductBatch lote = ProductBatch.builder().id(batchId).product(produto)
                .lote("L1").validade(LocalDate.now().plusMonths(1)).quantidade(10).build();
        Customer cliente = Customer.builder().id(UUID.randomUUID()).nome("Hospital Central").email("c@h.com").build();
        Order order = Order.builder().id(UUID.randomUUID()).numeroPedido("PED-000123")
                .customer(cliente).status(OrderStatus.SEPARADO).valorTotal(new BigDecimal("100.00")).build();
        OrderItem item = OrderItem.builder().id(UUID.randomUUID()).order(order).product(produto).quantidade(20).build();
        OrderItemBatch alocacao = OrderItemBatch.builder().id(UUID.randomUUID())
                .orderItem(item).batch(lote).quantidadeConsumida(20).build();

        when(productBatchRepository.existsById(batchId)).thenReturn(true);
        when(orderItemBatchRepository.findByBatchId(batchId)).thenReturn(List.of(alocacao));

        List<BatchOrderTraceResponse> result = batchService.findOrdersForBatch(batchId);

        assertEquals(1, result.size());
        assertEquals("PED-000123", result.get(0).numeroPedido());
        assertEquals("Hospital Central", result.get(0).customerNome());
        assertEquals("SEPARADO", result.get(0).status());
        assertEquals(20, result.get(0).quantidadeConsumida());
    }
}
