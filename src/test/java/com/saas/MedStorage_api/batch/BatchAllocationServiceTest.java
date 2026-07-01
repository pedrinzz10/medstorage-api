package com.saas.MedStorage_api.batch;

import com.saas.MedStorage_api.batch.entity.OrderItemBatch;
import com.saas.MedStorage_api.batch.entity.ProductBatch;
import com.saas.MedStorage_api.batch.repository.OrderItemBatchRepository;
import com.saas.MedStorage_api.batch.repository.ProductBatchRepository;
import com.saas.MedStorage_api.batch.service.BatchAllocationService;
import com.saas.MedStorage_api.exception.InsufficientStockException;
import com.saas.MedStorage_api.order.entity.OrderItem;
import com.saas.MedStorage_api.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchAllocationServiceTest {

    @Mock
    private ProductBatchRepository productBatchRepository;
    @Mock
    private OrderItemBatchRepository orderItemBatchRepository;

    @InjectMocks
    private BatchAllocationService batchAllocationService;

    private Product luva;
    private OrderItem item;

    @BeforeEach
    void setUp() {
        luva = Product.builder().id(UUID.randomUUID()).nome("Luva Nitrílica").build();
        item = OrderItem.builder().id(UUID.randomUUID()).product(luva).quantidade(50).build();

        lenientSaveEcho();
    }

    private void lenientSaveEcho() {
        org.mockito.Mockito.lenient().when(productBatchRepository.save(any(ProductBatch.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient().when(orderItemBatchRepository.save(any(OrderItemBatch.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private ProductBatch batch(String lote, int diasValidade, int quantidade) {
        return ProductBatch.builder().id(UUID.randomUUID()).product(luva)
                .lote(lote).validade(LocalDate.now().plusDays(diasValidade)).quantidade(quantidade).build();
    }

    @Test
    void allocateFefo_withoutAnyBatch_doesNothing() {
        when(productBatchRepository.findByProductIdOrderByValidadeAsc(luva.getId())).thenReturn(List.of());

        batchAllocationService.allocateFefo(item);

        verify(orderItemBatchRepository, never()).save(any());
    }

    @Test
    void allocateFefo_withSingleBatchCoveringQuantity_consumesFromThatBatch() {
        ProductBatch b1 = batch("L1", 30, 100);
        when(productBatchRepository.findByProductIdOrderByValidadeAsc(luva.getId())).thenReturn(List.of(b1));

        batchAllocationService.allocateFefo(item);

        assertEquals(50, b1.getQuantidade());
        ArgumentCaptor<OrderItemBatch> captor = ArgumentCaptor.forClass(OrderItemBatch.class);
        verify(orderItemBatchRepository).save(captor.capture());
        assertEquals(50, captor.getValue().getQuantidadeConsumida());
        assertEquals(b1, captor.getValue().getBatch());
    }

    @Test
    void allocateFefo_splitsAcrossMultipleBatchesInFefoOrder() {
        ProductBatch venceLogo = batch("L-EARLY", 10, 30);
        ProductBatch venceDepois = batch("L-LATE", 60, 100);
        when(productBatchRepository.findByProductIdOrderByValidadeAsc(luva.getId()))
                .thenReturn(List.of(venceLogo, venceDepois));

        batchAllocationService.allocateFefo(item);

        assertEquals(0, venceLogo.getQuantidade());
        assertEquals(80, venceDepois.getQuantidade());

        ArgumentCaptor<OrderItemBatch> captor = ArgumentCaptor.forClass(OrderItemBatch.class);
        verify(orderItemBatchRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<OrderItemBatch> saved = captor.getAllValues();
        assertEquals(30, saved.get(0).getQuantidadeConsumida());
        assertEquals(venceLogo, saved.get(0).getBatch());
        assertEquals(20, saved.get(1).getQuantidadeConsumida());
        assertEquals(venceDepois, saved.get(1).getBatch());
    }

    @Test
    void allocateFefo_withInsufficientTotalAcrossBatches_throwsAndSkipsEmptyBatches() {
        ProductBatch vazio = batch("L-VAZIO", 5, 0);
        ProductBatch insuficiente = batch("L-POUCO", 20, 10);
        when(productBatchRepository.findByProductIdOrderByValidadeAsc(luva.getId()))
                .thenReturn(List.of(vazio, insuficiente));

        assertThrows(InsufficientStockException.class, () -> batchAllocationService.allocateFefo(item));
    }

    @Test
    void releaseAllocation_restoresQuantityAndDeletesAllocationRecords() {
        ProductBatch b1 = batch("L1", 30, 50);
        OrderItemBatch alocacao = OrderItemBatch.builder().id(UUID.randomUUID())
                .orderItem(item).batch(b1).quantidadeConsumida(20).build();
        when(orderItemBatchRepository.findByOrderItemId(item.getId())).thenReturn(List.of(alocacao));

        batchAllocationService.releaseAllocation(item);

        assertEquals(70, b1.getQuantidade());
        verify(productBatchRepository).save(b1);
        verify(orderItemBatchRepository).deleteAll(List.of(alocacao));
    }

    @Test
    void releaseAllocation_withNoAllocations_doesNothing() {
        when(orderItemBatchRepository.findByOrderItemId(item.getId())).thenReturn(List.of());

        batchAllocationService.releaseAllocation(item);

        verify(productBatchRepository, never()).save(any());
        verify(orderItemBatchRepository).deleteAll(List.of());
    }
}
