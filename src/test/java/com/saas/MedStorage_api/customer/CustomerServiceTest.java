package com.saas.MedStorage_api.customer;

import com.saas.MedStorage_api.customer.dto.CustomerRequest;
import com.saas.MedStorage_api.customer.dto.CustomerResponse;
import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.customer.repository.CustomerRepository;
import com.saas.MedStorage_api.customer.service.CustomerService;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer hospital;

    @BeforeEach
    void setUp() {
        hospital = Customer.builder()
                .id(UUID.randomUUID())
                .nome("Hospital Central")
                .email("compras@hospitalcentral.com")
                .telefone("11999999999")
                .cnpj("12345678901234")
                .build();
    }

    @Test
    void create_savesAndReturnsCustomer() {
        CustomerRequest request = new CustomerRequest(
                "Hospital Novo", "compras@novo.com", "11999999999", "12345678901234", null, null, Map.of());

        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        CustomerResponse response = customerService.create(request);

        assertEquals("Hospital Novo", response.nome());
        assertEquals("compras@novo.com", response.email());
    }

    @Test
    void findAll_returnsPagedCustomers() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Customer> page = new PageImpl<>(List.of(hospital), pageable, 1);
        when(customerRepository.findAll(pageable)).thenReturn(page);

        Page<CustomerResponse> result = customerService.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Hospital Central", result.getContent().get(0).nome());
    }

    @Test
    void findById_withExistingId_returnsCustomer() {
        when(customerRepository.findById(hospital.getId())).thenReturn(Optional.of(hospital));

        CustomerResponse response = customerService.findById(hospital.getId());

        assertEquals("Hospital Central", response.nome());
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(customerRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.findById(unknownId));
    }

    @Test
    void update_withExistingId_updatesFields() {
        CustomerRequest request = new CustomerRequest(
                "Hospital Renomeado", "novo@hospitalcentral.com", "11988887777", "12345678901234", "Rua X", "Maria", Map.of());

        when(customerRepository.findById(hospital.getId())).thenReturn(Optional.of(hospital));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.update(hospital.getId(), request);

        assertEquals("Hospital Renomeado", response.nome());
        assertEquals("novo@hospitalcentral.com", response.email());
        assertEquals("Maria", response.contatoPrincipal());
    }

    @Test
    void update_withUnknownId_throwsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        CustomerRequest request = new CustomerRequest("Nome", "email@test.com", null, null, null, null, null);
        when(customerRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.update(unknownId, request));
    }
}
