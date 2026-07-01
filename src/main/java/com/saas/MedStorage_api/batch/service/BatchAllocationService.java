package com.saas.MedStorage_api.batch.service;

import com.saas.MedStorage_api.batch.entity.OrderItemBatch;
import com.saas.MedStorage_api.batch.entity.ProductBatch;
import com.saas.MedStorage_api.batch.repository.OrderItemBatchRepository;
import com.saas.MedStorage_api.batch.repository.ProductBatchRepository;
import com.saas.MedStorage_api.exception.InsufficientStockException;
import com.saas.MedStorage_api.order.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Alocação FEFO (First-Expire-First-Out) de lotes ao separar um pedido, e
 * liberação dessa alocação se o pedido for cancelado depois de separado.
 *
 * Produtos sem nenhum lote cadastrado (estoque legado, recebido antes da
 * funcionalidade de lote/validade) não participam da alocação — o controle
 * de estoque agregado em Inventory continua sendo a única fonte de verdade
 * para eles, mantendo compatibilidade com pedidos e testes existentes.
 */
@Service
public class BatchAllocationService {

    private final ProductBatchRepository productBatchRepository;
    private final OrderItemBatchRepository orderItemBatchRepository;

    public BatchAllocationService(
            ProductBatchRepository productBatchRepository,
            OrderItemBatchRepository orderItemBatchRepository) {
        this.productBatchRepository = productBatchRepository;
        this.orderItemBatchRepository = orderItemBatchRepository;
    }

    public void allocateFefo(OrderItem item) {
        List<ProductBatch> lotes = productBatchRepository.findByProductIdOrderByValidadeAsc(item.getProduct().getId());
        if (lotes.isEmpty()) {
            return;
        }

        int restante = item.getQuantidade();
        for (ProductBatch lote : lotes) {
            if (restante <= 0) {
                break;
            }
            if (lote.getQuantidade() <= 0) {
                continue;
            }
            int consumida = Math.min(lote.getQuantidade(), restante);
            lote.setQuantidade(lote.getQuantidade() - consumida);
            productBatchRepository.save(lote);

            OrderItemBatch orderItemBatch = OrderItemBatch.builder()
                    .orderItem(item)
                    .batch(lote)
                    .quantidadeConsumida(consumida)
                    .build();
            orderItemBatchRepository.save(orderItemBatch);
            // O item ja pode estar em memoria na transacao atual (ex: prestes a
            // ser serializado na resposta) — sem isto a colecao lazy so
            // refletiria a insercao apos um flush, causando leitura vazia.
            item.getLotes().add(orderItemBatch);

            restante -= consumida;
        }

        if (restante > 0) {
            throw new InsufficientStockException(
                    "Nenhum lote com quantidade suficiente para o produto " + item.getProduct().getNome()
                    + ". Faltam " + restante + " unidade(s) para completar a separação.");
        }
    }

    public void releaseAllocation(OrderItem item) {
        List<OrderItemBatch> alocacoes = orderItemBatchRepository.findByOrderItemId(item.getId());
        for (OrderItemBatch alocacao : alocacoes) {
            ProductBatch lote = alocacao.getBatch();
            lote.setQuantidade(lote.getQuantidade() + alocacao.getQuantidadeConsumida());
            productBatchRepository.save(lote);
        }
        orderItemBatchRepository.deleteAll(alocacoes);
    }
}
