package com.saas.MedStorage_api.customer.dto;

import com.saas.MedStorage_api.customer.entity.Customer;

import java.util.Map;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        String cnpj,
        String endereco,
        String contatoPrincipal,
        Map<String, Object> dadosAdicionais
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getNome(),
                customer.getEmail(),
                customer.getTelefone(),
                customer.getCnpj(),
                customer.getEndereco(),
                customer.getContatoPrincipal(),
                customer.getDadosAdicionais());
    }
}
