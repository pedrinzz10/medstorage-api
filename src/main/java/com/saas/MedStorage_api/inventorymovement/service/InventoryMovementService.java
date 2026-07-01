package com.saas.MedStorage_api.inventorymovement.service;

import com.saas.MedStorage_api.batch.entity.ProductBatch;
import com.saas.MedStorage_api.batch.repository.ProductBatchRepository;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.inventory.entity.Inventory;
import com.saas.MedStorage_api.inventory.repository.InventoryRepository;
import com.saas.MedStorage_api.inventorymovement.dto.InventoryMovementResponse;
import com.saas.MedStorage_api.inventorymovement.dto.StockAdjustmentRequest;
import com.saas.MedStorage_api.inventorymovement.dto.StockCountRequest;
import com.saas.MedStorage_api.inventorymovement.entity.InventoryMovement;
import com.saas.MedStorage_api.inventorymovement.enums.MovementType;
import com.saas.MedStorage_api.inventorymovement.repository.InventoryMovementRepository;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryMovementService {

    private final InventoryMovementRepository movementRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final ProductBatchRepository productBatchRepository;

    public InventoryMovementService(
            InventoryMovementRepository movementRepository,
            InventoryRepository inventoryRepository,
            UserRepository userRepository,
            ProductBatchRepository productBatchRepository) {
        this.movementRepository = movementRepository;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
        this.productBatchRepository = productBatchRepository;
    }

    public Page<InventoryMovementResponse> findAll(UUID productId, Pageable pageable) {
        if (productId != null) {
            return movementRepository.findByProduct_Id(productId, pageable).map(InventoryMovementResponse::from);
        }
        return movementRepository.findAll(pageable).map(InventoryMovementResponse::from);
    }

    @Transactional
    public InventoryMovementResponse adjust(StockAdjustmentRequest request, Authentication authentication) {
        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product " + request.productId()));

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        inventory.setQuantidade(inventory.getQuantidade() + request.quantidade());
        inventoryRepository.save(inventory);

        ProductBatch batch = productBatchRepository.findByProductIdAndLote(request.productId(), request.lote())
                .orElseGet(() -> ProductBatch.builder()
                        .product(inventory.getProduct())
                        .lote(request.lote())
                        .validade(request.validade())
                        .quantidade(0)
                        .build());
        batch.setQuantidade(batch.getQuantidade() + request.quantidade());
        productBatchRepository.save(batch);

        InventoryMovement movement = InventoryMovement.builder()
                .product(inventory.getProduct())
                .tipo(MovementType.IN)
                .quantidade(request.quantidade())
                .motivo(request.motivo())
                .referenciaTipo("MANUAL")
                .criadoPor(user)
                .build();

        return InventoryMovementResponse.from(movementRepository.save(movement));
    }

    @Transactional
    public InventoryMovementResponse registerCount(StockCountRequest request, Authentication authentication) {
        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product " + request.productId()));

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int delta = request.quantidadeContada() - inventory.getQuantidade();
        if (delta == 0) {
            throw new BadRequestException("Nenhuma divergência encontrada na contagem");
        }

        inventory.setQuantidade(request.quantidadeContada());
        inventoryRepository.save(inventory);

        InventoryMovement movement = InventoryMovement.builder()
                .product(inventory.getProduct())
                .tipo(delta > 0 ? MovementType.IN : MovementType.OUT)
                .quantidade(Math.abs(delta))
                .motivo("Contagem cíclica: " + request.observacao())
                .referenciaTipo("CYCLE_COUNT")
                .criadoPor(user)
                .build();

        return InventoryMovementResponse.from(movementRepository.save(movement));
    }
}
