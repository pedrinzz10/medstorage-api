package com.saas.MedStorage_api.product.service;

import com.saas.MedStorage_api.product.dto.ProductAbcResponse;
import com.saas.MedStorage_api.product.entity.ProductSalesView;
import com.saas.MedStorage_api.product.repository.ProductSalesViewRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Classificação ABC (Pareto) de produtos por valor vendido em pedidos
 * FINALIZADO: classe A cobre até 80% do valor acumulado, B até 95%,
 * C o restante (inclui produtos sem nenhuma venda).
 */
@Service
public class AbcAnalysisService {

    private static final BigDecimal LIMITE_A = BigDecimal.valueOf(80);
    private static final BigDecimal LIMITE_B = BigDecimal.valueOf(95);

    private final ProductSalesViewRepository productSalesViewRepository;

    public AbcAnalysisService(ProductSalesViewRepository productSalesViewRepository) {
        this.productSalesViewRepository = productSalesViewRepository;
    }

    public List<ProductAbcResponse> classify() {
        List<ProductSalesView> vendas = new ArrayList<>(productSalesViewRepository.findAll());
        vendas.sort(Comparator.comparing(ProductSalesView::getValorVendido).reversed()
                .thenComparing(ProductSalesView::getNome));

        BigDecimal totalGeral = vendas.stream()
                .map(ProductSalesView::getValorVendido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ProductAbcResponse> resultado = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (ProductSalesView venda : vendas) {
            acumulado = acumulado.add(venda.getValorVendido());
            BigDecimal percentualAcumulado = totalGeral.signum() == 0
                    ? BigDecimal.ZERO
                    : acumulado.divide(totalGeral, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);

            String classe = venda.getValorVendido().signum() == 0
                    ? "C"
                    : classificar(percentualAcumulado);

            resultado.add(new ProductAbcResponse(
                    venda.getProductId(),
                    venda.getNome(),
                    venda.getSku(),
                    venda.getValorVendido(),
                    venda.getQuantidadeVendida(),
                    percentualAcumulado,
                    classe));
        }
        return resultado;
    }

    private String classificar(BigDecimal percentualAcumulado) {
        if (percentualAcumulado.compareTo(LIMITE_A) <= 0) {
            return "A";
        }
        if (percentualAcumulado.compareTo(LIMITE_B) <= 0) {
            return "B";
        }
        return "C";
    }
}
