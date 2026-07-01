package com.saas.MedStorage_api.consignment.dto;

import com.saas.MedStorage_api.consignment.entity.Consignment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConsignmentResponse(
        UUID id,
        UUID customerId,
        String customerNome,
        String status,
        String observacoes,
        LocalDateTime createdAt,
        List<ConsignmentItemResponse> items
) {
    public static ConsignmentResponse from(Consignment consignment) {
        return new ConsignmentResponse(
                consignment.getId(),
                consignment.getCustomer().getId(),
                consignment.getCustomer().getNome(),
                consignment.getStatus().name(),
                consignment.getObservacoes(),
                consignment.getCreatedAt(),
                consignment.getItems().stream().map(ConsignmentItemResponse::from).toList());
    }
}
