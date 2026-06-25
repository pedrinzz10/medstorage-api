package com.saas.MedStorage_api.customer.service;

import com.saas.MedStorage_api.customer.dto.CustomerRequest;
import com.saas.MedStorage_api.customer.dto.CustomerResponse;
import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.customer.repository.CustomerRepository;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
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

    public CustomerResponse findById(UUID id) {
        return CustomerResponse.from(getOrThrow(id));
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

    private Customer getOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }
}
