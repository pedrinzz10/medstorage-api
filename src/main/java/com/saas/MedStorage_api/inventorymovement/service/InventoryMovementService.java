package com.saas.MedStorage_api.inventorymovement.service;

import com.saas.MedStorage_api.inventorymovement.dto.InventoryMovementResponse;
import com.saas.MedStorage_api.inventorymovement.repository.InventoryMovementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InventoryMovementService {

    private final InventoryMovementRepository movementRepository;

    public InventoryMovementService(InventoryMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    public Page<InventoryMovementResponse> findAll(UUID productId, Pageable pageable) {
        if (productId != null) {
            return movementRepository.findByProduct_Id(productId, pageable).map(InventoryMovementResponse::from);
        }
        return movementRepository.findAll(pageable).map(InventoryMovementResponse::from);
    }
}
