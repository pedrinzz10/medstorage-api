package com.saas.MedStorage_api.domain.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    @Query("select i from Inventory i join fetch i.product p where p.ativo = true")
    List<Inventory> findAllForActiveProducts();
}
