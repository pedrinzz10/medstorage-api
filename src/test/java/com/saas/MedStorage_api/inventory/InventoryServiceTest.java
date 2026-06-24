package com.saas.MedStorage_api.inventory;

import com.saas.MedStorage_api.domain.inventory.Inventory;
import com.saas.MedStorage_api.domain.inventory.InventoryRepository;
import com.saas.MedStorage_api.domain.product.Product;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.inventory.dto.InventoryStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory inventoryWith(String nome, int quantidade, int estoqueMinimo) {
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .sku(nome.substring(0, 3).toUpperCase())
                .precoBase(BigDecimal.ONE)
                .estoqueMinimo(estoqueMinimo)
                .ativo(true)
                .build();

        return Inventory.builder()
                .id(UUID.randomUUID())
                .product(product)
                .quantidade(quantidade)
                .build();
    }

    @Test
    void getStatus_classifiesEachSeverityCorrectly() {
Inventory critico = inventoryWith("Gaze", 50, 50);   // quantidade <= minimo
        Inventory baixo = inventoryWith("Mascara", 120, 100); // quantidade <= minimo * 1.5
        Inventory ok = inventoryWith("Alcool", 1000, 80);     // muito acima do minimo

        when(inventoryRepository.findAllForActiveProducts()).thenReturn(List.of(critico, baixo, ok));

        List<InventoryStatusResponse> result = inventoryService.getStatus();

        assertEquals(3, result.size());
        assertEquals("CRITICO", result.get(0).statusEstoque());
        assertEquals("BAIXO", result.get(1).statusEstoque());
        assertEquals("OK", result.get(2).statusEstoque());
    }

    @Test
    void findByProductId_withExistingProduct_returnsStatus() {
        inventoryService = new InventoryService(inventoryRepository);
        Inventory inventory = inventoryWith("Luva", 1000, 100);

        when(inventoryRepository.findByProductId(inventory.getProduct().getId()))
                .thenReturn(Optional.of(inventory));

        InventoryStatusResponse response = inventoryService.findByProductId(inventory.getProduct().getId());

        assertEquals("OK", response.statusEstoque());
        assertEquals(1000, response.quantidadeAtual());
    }

    @Test
    void findByProductId_withUnknownProduct_throwsResourceNotFoundException() {
        inventoryService = new InventoryService(inventoryRepository);
        UUID unknownId = UUID.randomUUID();

        when(inventoryRepository.findByProductId(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.findByProductId(unknownId));
    }
}
