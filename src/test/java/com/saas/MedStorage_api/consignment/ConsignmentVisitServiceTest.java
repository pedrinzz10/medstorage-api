package com.saas.MedStorage_api.consignment;

import com.saas.MedStorage_api.consignment.dto.CreateVisitRequest;
import com.saas.MedStorage_api.consignment.dto.VisitResponse;
import com.saas.MedStorage_api.consignment.entity.ConsignmentVisit;
import com.saas.MedStorage_api.consignment.enums.VisitStatus;
import com.saas.MedStorage_api.consignment.repository.ConsignmentVisitRepository;
import com.saas.MedStorage_api.consignment.service.ConsignmentVisitService;
import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.customer.repository.CustomerRepository;
import com.saas.MedStorage_api.exception.BadRequestException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsignmentVisitServiceTest {

    @Mock private ConsignmentVisitRepository visitRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;
    @Mock private Authentication authentication;

    @InjectMocks
    private ConsignmentVisitService visitService;

    private Customer customer;
    private User funcionario;
    private User gerente;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(UUID.randomUUID()).nome("Hospital Central").build();
        funcionario = User.builder().id(UUID.randomUUID()).nome("Fulano").email("fulano@distribuidor.com").role(UserRole.GERENTE_ESTOQUE).build();
        gerente = User.builder().id(UUID.randomUUID()).email("gerente@distribuidor.com").role(UserRole.GERENTE_ESTOQUE).build();

        lenient().when(authentication.getName()).thenReturn(gerente.getEmail());
        lenient().when(userRepository.findByEmail(gerente.getEmail())).thenReturn(Optional.of(gerente));
        lenient().when(visitRepository.save(any(ConsignmentVisit.class))).thenAnswer(inv -> {
            ConsignmentVisit v = inv.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });
    }

    @Test
    void create_withValidRequest_returnsScheduledVisit() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(userRepository.findById(funcionario.getId())).thenReturn(Optional.of(funcionario));
        CreateVisitRequest request = new CreateVisitRequest(customer.getId(), funcionario.getId(), LocalDate.now().plusDays(5), "Levar balança");

        VisitResponse response = visitService.create(request, authentication);

        assertEquals("AGENDADA", response.status());
        assertEquals(customer.getId(), response.customerId());
        assertEquals(funcionario.getId(), response.funcionarioId());
    }

    @Test
    void findByRange_returnsSortedByDate() {
        ConsignmentVisit v1 = ConsignmentVisit.builder().id(UUID.randomUUID()).customer(customer).funcionario(funcionario)
                .dataAgendada(LocalDate.now().plusDays(10)).status(VisitStatus.AGENDADA).build();
        ConsignmentVisit v2 = ConsignmentVisit.builder().id(UUID.randomUUID()).customer(customer).funcionario(funcionario)
                .dataAgendada(LocalDate.now().plusDays(2)).status(VisitStatus.AGENDADA).build();
        when(visitRepository.findByDataAgendadaBetween(any(), any())).thenReturn(List.of(v1, v2));

        List<VisitResponse> result = visitService.findByRange(LocalDate.now(), LocalDate.now().plusDays(30));

        assertEquals(2, result.size());
        assertEquals(v2.getId(), result.get(0).id());
        assertEquals(v1.getId(), result.get(1).id());
    }

    @Test
    void cancel_withScheduledVisit_setsCancelada() {
        ConsignmentVisit visit = ConsignmentVisit.builder().id(UUID.randomUUID()).customer(customer).funcionario(funcionario)
                .dataAgendada(LocalDate.now().plusDays(3)).status(VisitStatus.AGENDADA).build();
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        VisitResponse response = visitService.cancel(visit.getId());

        assertEquals("CANCELADA", response.status());
    }

    @Test
    void cancel_withAlreadyRealizedVisit_throws() {
        ConsignmentVisit visit = ConsignmentVisit.builder().id(UUID.randomUUID()).customer(customer).funcionario(funcionario)
                .dataAgendada(LocalDate.now().minusDays(1)).status(VisitStatus.REALIZADA).build();
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThrows(BadRequestException.class, () -> visitService.cancel(visit.getId()));
    }

    @Test
    void markRealizada_withScheduledVisit_updatesStatus() {
        ConsignmentVisit visit = ConsignmentVisit.builder().id(UUID.randomUUID()).customer(customer).funcionario(funcionario)
                .dataAgendada(LocalDate.now()).status(VisitStatus.AGENDADA).build();
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        visitService.markRealizada(visit.getId());

        assertEquals(VisitStatus.REALIZADA, visit.getStatus());
    }

    @Test
    void markRealizada_withCancelledVisit_throws() {
        ConsignmentVisit visit = ConsignmentVisit.builder().id(UUID.randomUUID()).customer(customer).funcionario(funcionario)
                .dataAgendada(LocalDate.now()).status(VisitStatus.CANCELADA).build();
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThrows(BadRequestException.class, () -> visitService.markRealizada(visit.getId()));
    }
}
