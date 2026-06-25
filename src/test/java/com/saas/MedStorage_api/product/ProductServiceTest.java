package com.saas.MedStorage_api.product;

import com.saas.MedStorage_api.product.Product;
import com.saas.MedStorage_api.product.ProductRepository;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.product.dto.ProductResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

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
}
