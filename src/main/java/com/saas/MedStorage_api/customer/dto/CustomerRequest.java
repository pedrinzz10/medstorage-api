package com.saas.MedStorage_api.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CustomerRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        String telefone,
        String cnpj,
        String endereco,
        String contatoPrincipal,
        Map<String, Object> dadosAdicionais
) {
}
