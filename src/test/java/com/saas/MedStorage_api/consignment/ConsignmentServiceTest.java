package com.saas.MedStorage_api.consignment;

import com.saas.MedStorage_api.batch.entity.ProductBatch;
import com.saas.MedStorage_api.batch.repository.ProductBatchRepository;
import com.saas.MedStorage_api.consignment.dto.ConsignmentItemRequest;
import com.saas.MedStorage_api.consignment.dto.ConsignmentItemResponse;
import com.saas.MedStorage_api.consignment.dto.ConsignmentResponse;
import com.saas.MedStorage_api.consignment.dto.ConsignmentUsageResponse;
import com.saas.MedStorage_api.consignment.dto.CreateConsignmentRequest;
import com.saas.MedStorage_api.consignment.dto.RegisterReturnRequest;
import com.saas.MedStorage_api.consignment.dto.RegisterUsageRequest;
import com.saas.MedStorage_api.consignment.entity.Consignment;
import com.saas.MedStorage_api.consignment.entity.ConsignmentItem;
import com.saas.MedStorage_api.consignment.enums.ConsignmentStatus;
import com.saas.MedStorage_api.consignment.repository.ConsignmentItemRepository;
import com.saas.MedStorage_api.consignment.repository.ConsignmentRepository;
import com.saas.MedStorage_api.consignment.repository.ConsignmentUsageRepository;
import com.saas.MedStorage_api.consignment.service.ConsignmentService;
import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.customer.repository.CustomerRepository;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.InsufficientStockException;
import com.saas.MedStorage_api.inventory.entity.Inventory;
import com.saas.MedStorage_api.inventory.repository.InventoryRepository;
import com.saas.MedStorage_api.inventorymovement.repository.InventoryMovementRepository;
import com.saas.MedStorage_api.product.entity.Product;
import com.saas.MedStorage_api.product.repository.ProductRepository;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import com.saas.MedStorage_api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsignmentServiceTest {

    @Mock private ConsignmentRepository consignmentRepository;
    @Mock private ConsignmentItemRepository consignmentItemRepository;
    @Mock private ConsignmentUsageRepository consignmentUsageRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductBatchRepository productBatchRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryMovementRepository movementRepository;
    @Mock private UserRepository userRepository;
    @Mock private Authentication authentication;

    @InjectMocks
    private ConsignmentService consignmentService;

    private Customer customer;
    private User vendedor;
    private Product luva;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(UUID.randomUUID()).nome("Hospital Central").email("c@h.com").build();
        vendedor = User.builder().id(UUID.randomUUID()).email("vendedor1@distribuidor.com").role(UserRole.VENDEDOR).build();
        luva = Product.builder().id(UUID.randomUUID()).nome("Luva").precoBase(new BigDecimal("10.00")).build();
        inventory = Inventory.builder().id(UUID.randomUUID()).product(luva).quantidade(100).quantidadeReservada(0).build();

        lenient().when(authentication.getName()).thenReturn(vendedor.getEmail());
        lenient().when(userRepository.findByEmail(vendedor.getEmail())).thenReturn(Optional.of(vendedor));
        lenient().when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        lenient().when(productRepository.findById(luva.getId())).thenReturn(Optional.of(luva));
        lenient().when(inventoryRepository.findByProductId(luva.getId())).thenReturn(Optional.of(inventory));
        lenient().when(consignmentRepository.saveAndFlush(any(Consignment.class)))
                .thenAnswer(inv -> {
                    Consignment c = inv.getArgument(0);
                    if (c.getId() == null) c.setId(UUID.randomUUID());
                    return c;
                });
        lenient().when(consignmentRepository.save(any(Consignment.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(productBatchRepository.save(any(ProductBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(consignmentItemRepository.save(any(ConsignmentItem.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(consignmentUsageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ProductBatch batch(String lote, int quantidade) {
        return ProductBatch.builder().id(UUID.randomUUID()).product(luva).lote(lote).validade(LocalDate.now().plusYears(1)).quantidade(quantidade).build();
    }

    @Test
    void create_withSingleBatchCoveringQuantity_decrementsBatchAndInventory() {
        ProductBatch b = batch("L1", 50);
        when(productBatchRepository.findByProductIdOrderByValidadeAsc(luva.getId())).thenReturn(List.of(b));
        CreateConsignmentRequest request = new CreateConsignmentRequest(
                customer.getId(), List.of(new ConsignmentItemRequest(luva.getId(), 20)), null);

        ConsignmentResponse response = consignmentService.create(request, authentication);

        assertEquals(1, response.items().size());
        assertEquals(30, b.getQuantidade());
        assertEquals(80, inventory.getQuantidade());
        assertEquals("L1", response.items().get(0).lote());
        assertEquals(20, response.items().get(0).saldoDisponivel());
    }

    @Test
    void create_withoutAnyBatch_decrementsInventoryOnlyAndSkipsBatchTracking() {
        when(productBatchRepository.findByProductIdOrderByValidadeAsc(luva.getId())).thenReturn(List.of());
        CreateConsignmentRequest request = new CreateConsignmentRequest(
                customer.getId(), List.of(new ConsignmentItemRequest(luva.getId(), 20)), null);

        ConsignmentResponse response = consignmentService.create(request, authentication);

        assertEquals(80, inventory.getQuantidade());
        assertEquals(null, response.items().get(0).lote());
    }

    @Test
    void create_withInsufficientAggregateStock_throwsAndDoesNotMutate() {
        // Nao precisa stubar productBatchRepository: a checagem de estoque agregado
        // acontece antes da escolha de lote e ja lanca a excecao.
        CreateConsignmentRequest request = new CreateConsignmentRequest(
                customer.getId(), List.of(new ConsignmentItemRequest(luva.getId(), 200)), null);

        assertThrows(InsufficientStockException.class, () -> consignmentService.create(request, authentication));
        assertEquals(100, inventory.getQuantidade());
    }

    @Test
    void create_withBatchesButNoneCoveringQuantity_throws() {
        when(productBatchRepository.findByProductIdOrderByValidadeAsc(luva.getId()))
                .thenReturn(List.of(batch("L1", 5), batch("L2", 8)));
        CreateConsignmentRequest request = new CreateConsignmentRequest(
                customer.getId(), List.of(new ConsignmentItemRequest(luva.getId(), 10)), null);

        assertThrows(InsufficientStockException.class, () -> consignmentService.create(request, authentication));
    }

    private ConsignmentItem itemComSaldo(int enviada, int usada, int devolvida) {
        Consignment consignment = Consignment.builder().id(UUID.randomUUID()).customer(customer)
                .status(ConsignmentStatus.ATIVO).build();
        ConsignmentItem item = ConsignmentItem.builder().id(UUID.randomUUID())
                .product(luva).quantidadeEnviada(enviada).quantidadeUsada(usada).quantidadeDevolvida(devolvida)
                .precoUnitario(luva.getPrecoBase()).build();
        consignment.addItem(item);
        lenient().when(consignmentItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        return item;
    }

    @Test
    void registerUsage_withinBalance_createsUsageAndIncrementsQuantidadeUsada() {
        ConsignmentItem item = itemComSaldo(20, 0, 0);
        RegisterUsageRequest request = new RegisterUsageRequest(5, LocalDate.now());

        ConsignmentUsageResponse response = consignmentService.registerUsage(item.getConsignment().getId(), item.getId(), request, authentication);

        assertEquals(5, response.quantidade());
        assertEquals(new BigDecimal("50.00"), response.valorFaturado());
        assertEquals(5, item.getQuantidadeUsada());
    }

    @Test
    void registerUsage_exceedingBalance_throwsAndDoesNotMutate() {
        ConsignmentItem item = itemComSaldo(10, 8, 0);
        RegisterUsageRequest request = new RegisterUsageRequest(5, LocalDate.now());

        assertThrows(BadRequestException.class,
                () -> consignmentService.registerUsage(item.getConsignment().getId(), item.getId(), request, authentication));
        assertEquals(8, item.getQuantidadeUsada());
    }

    @Test
    void registerUsage_whenBalanceReachesZero_autoClosesConsignment() {
        ConsignmentItem item = itemComSaldo(10, 0, 0);
        RegisterUsageRequest request = new RegisterUsageRequest(10, LocalDate.now());

        consignmentService.registerUsage(item.getConsignment().getId(), item.getId(), request, authentication);

        assertEquals(ConsignmentStatus.ENCERRADO, item.getConsignment().getStatus());
    }

    @Test
    void registerReturn_withinBalance_returnsToInventoryAndBatch() {
        ProductBatch b = batch("L1", 10);
        ConsignmentItem item = itemComSaldo(20, 0, 0);
        item.setBatch(b);
        RegisterReturnRequest request = new RegisterReturnRequest(5);

        ConsignmentItemResponse response = consignmentService.registerReturn(item.getConsignment().getId(), item.getId(), request, authentication);

        assertEquals(5, response.quantidadeDevolvida());
        assertEquals(15, b.getQuantidade());
        assertEquals(105, inventory.getQuantidade());
    }

    @Test
    void registerReturn_exceedingBalance_throws() {
        ConsignmentItem item = itemComSaldo(10, 3, 0);
        RegisterReturnRequest request = new RegisterReturnRequest(8);

        assertThrows(BadRequestException.class,
                () -> consignmentService.registerReturn(item.getConsignment().getId(), item.getId(), request, authentication));
    }

    @Test
    void close_manualClose_setsEncerrado() {
        Consignment consignment = Consignment.builder().id(UUID.randomUUID()).customer(customer)
                .status(ConsignmentStatus.ATIVO).build();
        when(consignmentRepository.findById(consignment.getId())).thenReturn(Optional.of(consignment));

        ConsignmentResponse response = consignmentService.close(consignment.getId());

        assertEquals("ENCERRADO", response.status());
    }
}
