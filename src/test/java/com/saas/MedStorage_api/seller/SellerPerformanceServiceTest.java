package com.saas.MedStorage_api.seller;

import com.saas.MedStorage_api.seller.dto.SellerPerformanceResponse;
import com.saas.MedStorage_api.seller.entity.SellerPerformanceView;
import com.saas.MedStorage_api.seller.repository.SellerPerformanceRepository;
import com.saas.MedStorage_api.seller.service.SellerPerformanceService;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import com.saas.MedStorage_api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerPerformanceServiceTest {

    @Mock private SellerPerformanceRepository performanceRepository;
    @Mock private UserRepository userRepository;
    @Mock private Authentication authentication;

    @InjectMocks
    private SellerPerformanceService performanceService;

    private User vendedor;
    private UUID vendedorId;

    @BeforeEach
    void setUp() {
        vendedorId = UUID.randomUUID();
        vendedor = User.builder()
                .id(vendedorId)
                .email("vendedor1@distribuidor.com")
                .nome("Vendedor Um")
                .role(UserRole.VENDEDOR)
                .build();

        lenient().when(authentication.getName()).thenReturn(vendedor.getEmail());
        lenient().when(userRepository.findByEmail(vendedor.getEmail())).thenReturn(Optional.of(vendedor));
    }

    @Test
    void getMyPerformance_withDataInCurrentMonth_returnsPerformance() {
        SellerPerformanceView view = buildView(vendedorId, 3, new BigDecimal("450.00"), 15);
        when(performanceRepository.findById(vendedorId)).thenReturn(Optional.of(view));

        SellerPerformanceResponse response = performanceService.getMyPerformance(authentication);

        assertEquals(vendedorId, response.vendedorId());
        assertEquals(3, response.totalPedidos());
        assertEquals(new BigDecimal("450.00"), response.valorVendido());
        assertEquals(15, response.quantidadeUnidades());
    }

    @Test
    void getMyPerformance_withNoOrdersThisMonth_returnsZeros() {
        when(performanceRepository.findById(vendedorId)).thenReturn(Optional.empty());

        SellerPerformanceResponse response = performanceService.getMyPerformance(authentication);

        assertEquals(vendedorId, response.vendedorId());
        assertEquals(vendedor.getNome(), response.vendedorNome());
        assertEquals(0, response.totalPedidos());
        assertEquals(BigDecimal.ZERO, response.valorVendido());
        assertEquals(0, response.quantidadeUnidades());
    }

    @Test
    void getAllPerformance_returnsAllSellers() {
        UUID outro = UUID.randomUUID();
        List<SellerPerformanceView> views = List.of(
                buildView(vendedorId, 2, new BigDecimal("200.00"), 8),
                buildView(outro, 1, new BigDecimal("100.00"), 3));
        when(performanceRepository.findAll()).thenReturn(views);

        List<SellerPerformanceResponse> responses = performanceService.getAllPerformance();

        assertEquals(2, responses.size());
        assertEquals(vendedorId, responses.get(0).vendedorId());
        assertEquals(outro, responses.get(1).vendedorId());
    }

    @Test
    void getAllPerformance_withNoData_returnsEmptyList() {
        when(performanceRepository.findAll()).thenReturn(List.of());

        List<SellerPerformanceResponse> responses = performanceService.getAllPerformance();

        assertEquals(0, responses.size());
    }

    private SellerPerformanceView buildView(UUID id, int totalPedidos, BigDecimal valor, int unidades) {
        SellerPerformanceView view = new SellerPerformanceView();
        setField(view, "vendedorId", id);
        setField(view, "vendedorNome", "Vendedor " + id.toString().substring(0, 4));
        setField(view, "vendedorEmail", "v" + id.toString().substring(0, 4) + "@test.com");
        setField(view, "totalPedidos", totalPedidos);
        setField(view, "valorVendido", valor);
        setField(view, "quantidadeUnidades", unidades);
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
}
