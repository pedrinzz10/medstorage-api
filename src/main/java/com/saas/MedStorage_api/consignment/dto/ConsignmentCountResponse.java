package com.saas.MedStorage_api.consignment.dto;

import com.saas.MedStorage_api.consignment.entity.ConsignmentCount;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ConsignmentCountResponse(
        UUID id,
        UUID customerId,
        String customerNome,
        UUID visitId,
        String funcionarioNome,
        LocalDate dataContagem,
        List<ConsignmentCountItemResponse> items
) {
    public static ConsignmentCountResponse from(ConsignmentCount count) {
        return new ConsignmentCountResponse(
                count.getId(),
                count.getCustomer().getId(),
                count.getCustomer().getNome(),
                count.getVisit() != null ? count.getVisit().getId() : null,
                count.getFuncionario().getNome(),
                count.getDataContagem(),
                count.getItems().stream().map(ConsignmentCountItemResponse::from).toList());
    }
}
