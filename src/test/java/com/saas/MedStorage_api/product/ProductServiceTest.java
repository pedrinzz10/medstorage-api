package com.saas.MedStorage_api.product;

import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.inventory.entity.Inventory;
import com.saas.MedStorage_api.inventory.repository.InventoryRepository;
import com.saas.MedStorage_api.product.dto.ProductRequest;
import com.saas.MedStorage_api.product.dto.ProductResponse;
import com.saas.MedStorage_api.product.entity.Product;
import com.saas.MedStorage_api.product.repository.ProductRepository;
import com.saas.MedStorage_api.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product luva;

    @BeforeEach
    void setUp() {
        luva = Product.builder()
                .id(UUID.randomUUID())
                .nome("Luva Cirurgica Tamanho M")
                .sku("LUV-M-001")
                .precoBase(new BigDecimal("0.50"))
                .unidade("par")
                .estoqueMinimo(100)
                .ativo(true)
                .build();
    }

    @Test
    void findAll_returnsPagedActiveProducts() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(luva), pageable, 1);
        when(productRepository.findByAtivoTrue(pageable)).thenReturn(page);

        Page<ProductResponse> result = productService.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("LUV-M-001", result.getContent().get(0).sku());
    }

    @Test
    void findById_withExistingId_returnsProduct() {
        when(productRepository.findById(luva.getId())).thenReturn(Optional.of(luva));

        ProductResponse response = productService.findById(luva.getId());

        assertEquals("Luva Cirurgica Tamanho M", response.nome());
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(productRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.findById(unknownId));
    }

    @Test
    void create_withValidRequest_savesProductAndInventory() {
        ProductRequest request = new ProductRequest(
                "Seringa 5ml", "Seringa descartável", "SER-5ML-001",
                new BigDecimal("0.80"), "unidade", 50, true);
        when(productRepository.existsBySku("SER-5ML-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.create(request);

        assertEquals("Seringa 5ml", response.nome());
        assertEquals("SER-5ML-001", response.sku());
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void create_withDuplicateSku_throwsBadRequestException() {
        ProductRequest request = new ProductRequest(
                "Luva Cópia", null, "LUV-M-001",
                new BigDecimal("0.50"), null, null, true);
        when(productRepository.existsBySku("LUV-M-001")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> productService.create(request));
    }

    @Test
    void update_withExistingId_updatesFields() {
        ProductRequest request = new ProductRequest(
                "Luva Renomeada", null, "LUV-M-001",
                new BigDecimal("0.60"), "par", 150, true);
        when(productRepository.findById(luva.getId())).thenReturn(Optional.of(luva));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.update(luva.getId(), request);

        assertEquals("Luva Renomeada", response.nome());
        assertEquals(new BigDecimal("0.60"), response.precoBase());
    }

    @Test
    void update_withDuplicateSku_throwsBadRequestException() {
        ProductRequest request = new ProductRequest(
                "Luva", null, "SKU-OUTRO",
                new BigDecimal("0.50"), null, null, true);
        when(productRepository.findById(luva.getId())).thenReturn(Optional.of(luva));
        when(productRepository.existsBySku("SKU-OUTRO")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> productService.update(luva.getId(), request));
    }

    @Test
    void deactivate_withExistingId_setsAtivoFalse() {
        when(productRepository.findById(luva.getId())).thenReturn(Optional.of(luva));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.deactivate(luva.getId());

        assertFalse(luva.isAtivo());
        verify(productRepository).save(luva);
    }

    @Test
    void deactivate_withUnknownId_throwsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(productRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.deactivate(unknownId));
    }
}
