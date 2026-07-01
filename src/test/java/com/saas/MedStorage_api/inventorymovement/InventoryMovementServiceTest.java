package com.saas.MedStorage_api.inventorymovement;

import com.saas.MedStorage_api.batch.entity.ProductBatch;
import com.saas.MedStorage_api.batch.repository.ProductBatchRepository;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.inventory.entity.Inventory;
import com.saas.MedStorage_api.inventory.repository.InventoryRepository;
import com.saas.MedStorage_api.inventorymovement.dto.InventoryMovementResponse;
import com.saas.MedStorage_api.inventorymovement.dto.StockAdjustmentRequest;
import com.saas.MedStorage_api.inventorymovement.dto.StockCountRequest;
import com.saas.MedStorage_api.inventorymovement.entity.InventoryMovement;
import com.saas.MedStorage_api.inventorymovement.enums.MovementType;
import com.saas.MedStorage_api.inventorymovement.repository.InventoryMovementRepository;
import com.saas.MedStorage_api.inventorymovement.service.InventoryMovementService;
import com.saas.MedStorage_api.product.entity.Product;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import com.saas.MedStorage_api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryMovementServiceTest {

    @Mock
    private InventoryMovementRepository movementRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductBatchRepository productBatchRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private InventoryMovementService movementService;

    private Product luva;
    private Inventory inventory;
    private User gerente;

    @BeforeEach
    void setUp() {
        luva = Product.builder().id(UUID.randomUUID()).nome("Luva Nitrílica").sku("LUV-001").build();
        inventory = Inventory.builder().id(UUID.randomUUID()).product(luva).quantidade(100).quantidadeReservada(0).build();
        gerente = User.builder().id(UUID.randomUUID()).email("gerente@distribuidor.com").role(UserRole.GERENTE_ESTOQUE).ativo(true).build();

        lenient().when(inventoryRepository.findByProductId(luva.getId())).thenReturn(Optional.of(inventory));
        lenient().when(userRepository.findByEmail(gerente.getEmail())).thenReturn(Optional.of(gerente));
        lenient().when(authentication.getName()).thenReturn(gerente.getEmail());
        lenient().when(movementRepository.save(any(InventoryMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(productBatchRepository.save(any(ProductBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void adjust_withNewLote_createsBatchWithRequestedQuantity() {
        when(productBatchRepository.findByProductIdAndLote(luva.getId(), "LOTE-001")).thenReturn(Optional.empty());
        StockAdjustmentRequest request = new StockAdjustmentRequest(luva.getId(), 20, "Compra", "LOTE-001", LocalDate.now().plusYears(1));

        movementService.adjust(request, authentication);

        ArgumentCaptor<ProductBatch> captor = ArgumentCaptor.forClass(ProductBatch.class);
        verify(productBatchRepository).save(captor.capture());
        assertEquals("LOTE-001", captor.getValue().getLote());
        assertEquals(20, captor.getValue().getQuantidade());
    }

    @Test
    void adjust_withExistingLote_sumsQuantityOnTopOfBatch() {
        ProductBatch existente = ProductBatch.builder().id(UUID.randomUUID()).product(luva)
                .lote("LOTE-002").validade(LocalDate.now().plusMonths(6)).quantidade(10).build();
        when(productBatchRepository.findByProductIdAndLote(luva.getId(), "LOTE-002")).thenReturn(Optional.of(existente));
        StockAdjustmentRequest request = new StockAdjustmentRequest(luva.getId(), 15, "Compra", "LOTE-002", LocalDate.now().plusYears(1));

        movementService.adjust(request, authentication);

        assertEquals(25, existente.getQuantidade());
        verify(productBatchRepository).save(existente);
    }

    @Test
    void registerCount_withHigherCount_createsInMovementWithDelta() {
        StockCountRequest request = new StockCountRequest(luva.getId(), 130, "Contagem mensal");

        InventoryMovementResponse response = movementService.registerCount(request, authentication);

        assertEquals("IN", response.tipo());
        assertEquals(30, response.quantidade());
        assertEquals(130, inventory.getQuantidade());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void registerCount_withLowerCount_createsOutMovementWithDelta() {
        StockCountRequest request = new StockCountRequest(luva.getId(), 70, "Avaria encontrada");

        InventoryMovementResponse response = movementService.registerCount(request, authentication);

        assertEquals("OUT", response.tipo());
        assertEquals(30, response.quantidade());
        assertEquals(70, inventory.getQuantidade());
    }

    @Test
    void registerCount_withSameCount_throwsBadRequestAndDoesNotSave() {
        StockCountRequest request = new StockCountRequest(luva.getId(), 100, "Sem divergência");

        assertThrows(BadRequestException.class, () -> movementService.registerCount(request, authentication));

        verify(inventoryRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void registerCount_setsMotivoAndReferenciaTipo() {
        StockCountRequest request = new StockCountRequest(luva.getId(), 130, "Contagem mensal");
        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);

        movementService.registerCount(request, authentication);

        verify(movementRepository).save(captor.capture());
        InventoryMovement saved = captor.getValue();
        assertEquals("Contagem cíclica: Contagem mensal", saved.getMotivo());
        assertEquals("CYCLE_COUNT", saved.getReferenciaTipo());
        assertEquals(MovementType.IN, saved.getTipo());
    }
}
