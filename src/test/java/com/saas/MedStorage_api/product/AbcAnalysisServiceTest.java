package com.saas.MedStorage_api.product;

import com.saas.MedStorage_api.product.dto.ProductAbcResponse;
import com.saas.MedStorage_api.product.entity.ProductSalesView;
import com.saas.MedStorage_api.product.repository.ProductSalesViewRepository;
import com.saas.MedStorage_api.product.service.AbcAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbcAnalysisServiceTest {

    @Mock
    private ProductSalesViewRepository productSalesViewRepository;

    private AbcAnalysisService abcAnalysisService;

    @BeforeEach
    void setUp() {
        abcAnalysisService = new AbcAnalysisService(productSalesViewRepository);
    }

    private ProductSalesView view(String nome, String sku, long valor, int quantidade) {
        ProductSalesView view = new ProductSalesView();
        setField(view, "productId", UUID.randomUUID());
        setField(view, "nome", nome);
        setField(view, "sku", sku);
        setField(view, "valorVendido", BigDecimal.valueOf(valor));
        setField(view, "quantidadeVendida", quantidade);
        return view;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void classify_splitsProductsIntoABCByAccumulatedPercentage() {
        when(productSalesViewRepository.findAll()).thenReturn(List.of(
                view("Produto A", "SKU-A", 800, 80),
                view("Produto B", "SKU-B", 150, 15),
                view("Produto C", "SKU-C", 50, 5),
                view("Produto D", "SKU-D", 0, 0)));

        List<ProductAbcResponse> result = abcAnalysisService.classify();

        assertEquals(4, result.size());
        assertEquals("Produto A", result.get(0).nome());
        assertEquals("A", result.get(0).classe());
        assertEquals(new BigDecimal("80.00"), result.get(0).percentualAcumulado());

        assertEquals("Produto B", result.get(1).nome());
        assertEquals("B", result.get(1).classe());
        assertEquals(new BigDecimal("95.00"), result.get(1).percentualAcumulado());

        assertEquals("Produto C", result.get(2).nome());
        assertEquals("C", result.get(2).classe());

        assertEquals("Produto D", result.get(3).nome());
        assertEquals("C", result.get(3).classe());
    }

    @Test
    void classify_withNoSalesAtAll_classifiesEveryoneAsC() {
        when(productSalesViewRepository.findAll()).thenReturn(List.of(
                view("Produto A", "SKU-A", 0, 0),
                view("Produto B", "SKU-B", 0, 0)));

        List<ProductAbcResponse> result = abcAnalysisService.classify();

        assertEquals(2, result.size());
        result.forEach(r -> {
            assertEquals("C", r.classe());
            assertEquals(BigDecimal.ZERO, r.percentualAcumulado());
        });
    }

    @Test
    void classify_withEmptyCatalog_returnsEmptyList() {
        when(productSalesViewRepository.findAll()).thenReturn(List.of());

        List<ProductAbcResponse> result = abcAnalysisService.classify();

        assertEquals(0, result.size());
    }
}
