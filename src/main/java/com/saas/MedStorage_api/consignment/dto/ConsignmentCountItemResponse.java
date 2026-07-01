package com.saas.MedStorage_api.consignment.dto;

import com.saas.MedStorage_api.consignment.entity.ConsignmentCountItem;

import java.time.LocalDate;
import java.util.UUID;

public record ConsignmentCountItemResponse(
        UUID id,
        UUID consignmentItemId,
        String productNome,
        int quantidadeContada,
        String loteConferido,
        LocalDate validadeConferida,
        int divergencia
) {
    public static ConsignmentCountItemResponse from(ConsignmentCountItem item) {
        return new ConsignmentCountItemResponse(
                item.getId(),
                item.getConsignmentItem().getId(),
                item.getConsignmentItem().getProduct().getNome(),
                item.getQuantidadeContada(),
                item.getLoteConferido(),
                item.getValidadeConferida(),
                item.getDivergencia());
    }
}
