package com.saas.MedStorage_api.inventory;

import com.saas.MedStorage_api.domain.inventory.Inventory;
import com.saas.MedStorage_api.domain.inventory.InventoryRepository;
import com.saas.MedStorage_api.domain.product.Product;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.inventory.dto.InventoryStatusResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private static final List<String> SEVERITY_ORDER = List.of("CRITICO", "BAIXO", "OK");

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<InventoryStatusResponse> getStatus() {
        return inventoryRepository.findAllForActiveProducts().stream()
                .map(this::toStatusResponse)
                .sorted(Comparator.comparing((InventoryStatusResponse r) -> SEVERITY_ORDER.indexOf(r.statusEstoque()))
                        .thenComparing(InventoryStatusResponse::nome))
                .toList();
    }

    public InventoryStatusResponse findByProductId(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product"));
        return toStatusResponse(inventory);
    }

    private InventoryStatusResponse toStatusResponse(Inventory inventory) {
        Product product = inventory.getProduct();
        return new InventoryStatusResponse(
                product.getId(),
                product.getNome(),
                product.getSku(),
                inventory.getQuantidade(),
                product.getEstoqueMinimo(),
                resolveStatus(inventory.getQuantidade(), product.getEstoqueMinimo()));
    }

    private String resolveStatus(int quantidade, Integer estoqueMinimo) {
        int minimo = estoqueMinimo == null ? 0 : estoqueMinimo;
        if (quantidade <= minimo) {
            return "CRITICO";
        }
        if (quantidade <= minimo * 1.5) {
            return "BAIXO";
        }
        return "OK";
    }
}
