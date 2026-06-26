package com.saas.MedStorage_api.commission;

import com.saas.MedStorage_api.commission.dto.CommissionResponse;
import com.saas.MedStorage_api.commission.entity.Commission;
import com.saas.MedStorage_api.commission.enums.CommissionStatus;
import com.saas.MedStorage_api.commission.repository.CommissionRepository;
import com.saas.MedStorage_api.commission.service.CommissionService;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock private CommissionRepository commissionRepository;

    @InjectMocks
    private CommissionService commissionService;

    private User vendedor;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        vendedor = User.builder()
                .id(UUID.randomUUID())
                .email("vendedor1@distribuidor.com")
                .nome("Vendedor Um")
                .role(UserRole.VENDEDOR)
                .build();
        pageable = PageRequest.of(0, 20);
    }

    @Test
    void findAll_withNoStatusFilter_returnsAllCommissions() {
        Commission c = buildCommission(CommissionStatus.PENDENTE);
        Page<Commission> page = new PageImpl<>(List.of(c));
        when(commissionRepository.findAll(pageable)).thenReturn(page);

        Page<CommissionResponse> result = commissionService.findAll(null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("PENDENTE", result.getContent().get(0).status());
        verify(commissionRepository).findAll(pageable);
    }

    @Test
    void findAll_withStatusFilter_delegatesToFindByStatus() {
        Commission c = buildCommission(CommissionStatus.PENDENTE);
        Page<Commission> page = new PageImpl<>(List.of(c));
        when(commissionRepository.findByStatus(CommissionStatus.PENDENTE, pageable)).thenReturn(page);

        Page<CommissionResponse> result = commissionService.findAll(CommissionStatus.PENDENTE, pageable);

        assertEquals(1, result.getTotalElements());
        verify(commissionRepository).findByStatus(CommissionStatus.PENDENTE, pageable);
    }

    @Test
    void findAll_withPagoFilter_returnsOnlyPaidCommissions() {
        Commission c = buildCommission(CommissionStatus.PAGO);
        Page<Commission> page = new PageImpl<>(List.of(c));
        when(commissionRepository.findByStatus(CommissionStatus.PAGO, pageable)).thenReturn(page);

        Page<CommissionResponse> result = commissionService.findAll(CommissionStatus.PAGO, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("PAGO", result.getContent().get(0).status());
    }

    @Test
    void findAll_withEmptyRepository_returnsEmptyPage() {
        when(commissionRepository.findAll(pageable)).thenReturn(Page.empty());

        Page<CommissionResponse> result = commissionService.findAll(null, pageable);

        assertEquals(0, result.getTotalElements());
    }

    private Commission buildCommission(CommissionStatus status) {
        return Commission.builder()
                .id(UUID.randomUUID())
                .vendedor(vendedor)
                .periodoInicio(LocalDate.now().withDayOfMonth(1))
                .periodoFim(LocalDate.now())
                .totalPedidos(5)
                .valorVendido(new BigDecimal("1000.00"))
                .quantidadeUnidades(20)
                .taxaComissao(new BigDecimal("10.00"))
                .status(status)
                .build();
    }
}
