package com.saas.MedStorage_api.inventorymovement.repository;

import com.saas.MedStorage_api.inventorymovement.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    Page<InventoryMovement> findByProduct_Id(UUID productId, Pageable pageable);
}
