package com.saas.MedStorage_api.consignment;

import com.saas.MedStorage_api.consignment.dto.ConsignmentCountResponse;
import com.saas.MedStorage_api.consignment.dto.CountItemRequest;
import com.saas.MedStorage_api.consignment.dto.RegisterCountRequest;
import com.saas.MedStorage_api.consignment.entity.Consignment;
import com.saas.MedStorage_api.consignment.entity.ConsignmentCount;
import com.saas.MedStorage_api.consignment.entity.ConsignmentItem;
import com.saas.MedStorage_api.consignment.entity.ConsignmentVisit;
import com.saas.MedStorage_api.consignment.enums.VisitStatus;
import com.saas.MedStorage_api.consignment.repository.ConsignmentCountRepository;
import com.saas.MedStorage_api.consignment.repository.ConsignmentItemRepository;
import com.saas.MedStorage_api.consignment.repository.ConsignmentVisitRepository;
import com.saas.MedStorage_api.consignment.service.ConsignmentCountService;
import com.saas.MedStorage_api.consignment.service.ConsignmentVisitService;
import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.customer.repository.CustomerRepository;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.product.entity.Product;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsignmentCountServiceTest {

    @Mock private ConsignmentCountRepository consignmentCountRepository;
    @Mock private ConsignmentItemRepository consignmentItemRepository;
    @Mock private ConsignmentVisitRepository consignmentVisitRepository;
    @Mock private ConsignmentVisitService consignmentVisitService;
    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;
    @Mock private Authentication authentication;

    @InjectMocks
    private ConsignmentCountService consignmentCountService;

    private Customer customer;
    private User gerente;
    private Product luva;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(UUID.randomUUID()).nome("Hospital Central").build();
        gerente = User.builder().id(UUID.randomUUID()).email("gerente@distribuidor.com").role(UserRole.GERENTE_ESTOQUE).build();
        luva = Product.builder().id(UUID.randomUUID()).nome("Luva").precoBase(new BigDecimal("10.00")).build();

        lenient().when(authentication.getName()).thenReturn(gerente.getEmail());
        lenient().when(userRepository.findByEmail(gerente.getEmail())).thenReturn(Optional.of(gerente));
        lenient().when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        lenient().when(consignmentCountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ConsignmentItem itemComSaldo(int enviada, int usada, int devolvida) {
        Consignment consignment = Consignment.builder().id(UUID.randomUUID()).customer(customer).build();
        ConsignmentItem item = ConsignmentItem.builder().id(UUID.randomUUID())
                .product(luva).quantidadeEnviada(enviada).quantidadeUsada(usada).quantidadeDevolvida(devolvida)
                .precoUnitario(luva.getPrecoBase()).build();
        consignment.addItem(item);
        lenient().when(consignmentItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        return item;
    }

    @Test
    void registerCount_withMissingMaterial_returnsPositiveDivergence() {
        ConsignmentItem item = itemComSaldo(20, 5, 0); // saldo esperado = 15
        RegisterCountRequest request = new RegisterCountRequest(customer.getId(), null, LocalDate.now(),
                List.of(new CountItemRequest(item.getId(), 10, "L1", LocalDate.now().plusYears(1))));

        ConsignmentCountResponse response = consignmentCountService.registerCount(request, authentication);

        assertEquals(1, response.items().size());
        assertEquals(5, response.items().get(0).divergencia());
    }

    @Test
    void registerCount_withMatchingCount_returnsZeroDivergence() {
        ConsignmentItem item = itemComSaldo(20, 5, 0); // saldo esperado = 15
        RegisterCountRequest request = new RegisterCountRequest(customer.getId(), null, LocalDate.now(),
                List.of(new CountItemRequest(item.getId(), 15, "L1", LocalDate.now().plusYears(1))));

        ConsignmentCountResponse response = consignmentCountService.registerCount(request, authentication);

        assertEquals(0, response.items().get(0).divergencia());
    }

    @Test
    void registerCount_withMoreThanExpected_returnsNegativeDivergence() {
        ConsignmentItem item = itemComSaldo(20, 5, 0); // saldo esperado = 15
        RegisterCountRequest request = new RegisterCountRequest(customer.getId(), null, LocalDate.now(),
                List.of(new CountItemRequest(item.getId(), 18, null, null)));

        ConsignmentCountResponse response = consignmentCountService.registerCount(request, authentication);

        assertEquals(-3, response.items().get(0).divergencia());
    }

    @Test
    void registerCount_withItemFromDifferentCustomer_throws() {
        Customer outroCustomer = Customer.builder().id(UUID.randomUUID()).nome("Outro Hospital").build();
        Consignment consignment = Consignment.builder().id(UUID.randomUUID()).customer(outroCustomer).build();
        ConsignmentItem item = ConsignmentItem.builder().id(UUID.randomUUID())
                .product(luva).quantidadeEnviada(10).precoUnitario(luva.getPrecoBase()).build();
        consignment.addItem(item);
        when(consignmentItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        RegisterCountRequest request = new RegisterCountRequest(customer.getId(), null, LocalDate.now(),
                List.of(new CountItemRequest(item.getId(), 5, null, null)));

        assertThrows(BadRequestException.class, () -> consignmentCountService.registerCount(request, authentication));
    }

    @Test
    void findByCustomer_withNullCustomerId_returnsAllCounts() {
        consignmentCountService.findByCustomer(null);

        org.mockito.Mockito.verify(consignmentCountRepository).findAllByOrderByDataContagemDesc();
        org.mockito.Mockito.verify(consignmentCountRepository, org.mockito.Mockito.never())
                .findByCustomer_IdOrderByDataContagemDesc(any());
    }

    @Test
    void findByCustomer_withCustomerId_filtersByCustomer() {
        consignmentCountService.findByCustomer(customer.getId());

        org.mockito.Mockito.verify(consignmentCountRepository).findByCustomer_IdOrderByDataContagemDesc(customer.getId());
        org.mockito.Mockito.verify(consignmentCountRepository, org.mockito.Mockito.never())
                .findAllByOrderByDataContagemDesc();
    }

    @Test
    void registerCount_withVisitId_marksVisitAsRealizada() {
        ConsignmentItem item = itemComSaldo(10, 0, 0);
        ConsignmentVisit visit = ConsignmentVisit.builder().id(UUID.randomUUID()).customer(customer)
                .funcionario(gerente).dataAgendada(LocalDate.now()).status(VisitStatus.AGENDADA).build();
        when(consignmentVisitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        RegisterCountRequest request = new RegisterCountRequest(customer.getId(), visit.getId(), LocalDate.now(),
                List.of(new CountItemRequest(item.getId(), 10, null, null)));

        consignmentCountService.registerCount(request, authentication);

        verify(consignmentVisitService).markRealizada(visit.getId());
    }
}
