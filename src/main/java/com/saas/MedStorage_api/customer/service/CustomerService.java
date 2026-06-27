package com.saas.MedStorage_api.customer.service;

import com.saas.MedStorage_api.customer.dto.CustomerDetailResponse;
import com.saas.MedStorage_api.customer.dto.CustomerRequest;
import com.saas.MedStorage_api.customer.dto.CustomerResponse;
import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.customer.entity.CustomerSummaryView;
import com.saas.MedStorage_api.customer.repository.CustomerRepository;
import com.saas.MedStorage_api.customer.repository.CustomerSummaryRepository;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.order.dto.OrderResponse;
import com.saas.MedStorage_api.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerSummaryRepository customerSummaryRepository;
    private final OrderRepository orderRepository;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerSummaryRepository customerSummaryRepository,
            OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.customerSummaryRepository = customerSummaryRepository;
        this.orderRepository = orderRepository;
    }

    public CustomerResponse create(CustomerRequest request) {
        Customer customer = Customer.builder()
                .nome(request.nome())
                .email(request.email())
                .telefone(request.telefone())
                .cnpj(request.cnpj())
                .endereco(request.endereco())
                .contatoPrincipal(request.contatoPrincipal())
                .dadosAdicionais(request.dadosAdicionais())
                .build();

        return CustomerResponse.from(customerRepository.save(customer));
    }

    public Page<CustomerResponse> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(CustomerResponse::from);
    }

    public CustomerDetailResponse findById(UUID id) {
        Customer customer = getOrThrow(id);
        CustomerSummaryView summary = customerSummaryRepository.findById(id).orElse(null);
        return CustomerDetailResponse.from(customer, summary);
    }

    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = getOrThrow(id);

        customer.setNome(request.nome());
        customer.setEmail(request.email());
        customer.setTelefone(request.telefone());
        customer.setCnpj(request.cnpj());
        customer.setEndereco(request.endereco());
        customer.setContatoPrincipal(request.contatoPrincipal());
        customer.setDadosAdicionais(request.dadosAdicionais());

        return CustomerResponse.from(customerRepository.save(customer));
    }

    public Page<OrderResponse> getOrders(UUID customerId, Pageable pageable) {
        getOrThrow(customerId);
        return orderRepository.findByCustomer_Id(customerId, pageable).map(OrderResponse::from);
    }

    private Customer getOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }
}
