package com.saas.MedStorage_api.consignment.dto;

import com.saas.MedStorage_api.consignment.entity.ConsignmentVisit;

import java.time.LocalDate;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        UUID customerId,
        String customerNome,
        UUID funcionarioId,
        String funcionarioNome,
        LocalDate dataAgendada,
        String status,
        String observacoes
) {
    public static VisitResponse from(ConsignmentVisit visit) {
        return new VisitResponse(
                visit.getId(),
                visit.getCustomer().getId(),
                visit.getCustomer().getNome(),
                visit.getFuncionario().getId(),
                visit.getFuncionario().getNome(),
                visit.getDataAgendada(),
                visit.getStatus().name(),
                visit.getObservacoes());
    }
}
