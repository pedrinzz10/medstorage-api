package com.saas.MedStorage_api.returns;

import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.inventory.entity.Inventory;
import com.saas.MedStorage_api.inventory.repository.InventoryRepository;
import com.saas.MedStorage_api.inventorymovement.repository.InventoryMovementRepository;
import com.saas.MedStorage_api.order.entity.Order;
import com.saas.MedStorage_api.order.entity.OrderItem;
import com.saas.MedStorage_api.order.enums.OrderStatus;
import com.saas.MedStorage_api.order.repository.OrderRepository;
import com.saas.MedStorage_api.product.entity.Product;
import com.saas.MedStorage_api.product.repository.ProductRepository;
import com.saas.MedStorage_api.returns.dto.CreateReturnRequest;
import com.saas.MedStorage_api.returns.dto.ReturnItemRequest;
import com.saas.MedStorage_api.returns.dto.ReturnResponse;
import com.saas.MedStorage_api.returns.entity.Return;
import com.saas.MedStorage_api.returns.entity.ReturnItem;
import com.saas.MedStorage_api.returns.enums.ReturnStatus;
import com.saas.MedStorage_api.returns.repository.ReturnRepository;
import com.saas.MedStorage_api.returns.service.ReturnService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock private ReturnRepository returnRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryMovementRepository movementRepository;
    @Mock private Authentication authentication;

    @InjectMocks
    private ReturnService returnService;

    private Customer customer;
    private User gerente;
    private Product luva;
    private Order finalizadoOrder;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(UUID.randomUUID()).nome("Hospital Central").email("c@h.com").build();
        gerente = User.builder().id(UUID.randomUUID()).email("gerente@distribuidor.com").role(UserRole.GERENTE_ESTOQUE).build();
        luva = Product.builder().id(UUID.randomUUID()).nome("Luva").precoBase(new BigDecimal("10.00")).build();

        finalizadoOrder = Order.builder()
                .id(UUID.randomUUID())
                .numeroPedido("PED-001000")
                .customer(customer)
                .status(OrderStatus.FINALIZADO)
                .valorTotal(new BigDecimal("50.00"))
                .dataConfirmado(LocalDateTime.now().minusDays(2))
                .dataSeparado(LocalDateTime.now().minusDays(2))
                .dataPronte(LocalDateTime.now().minusDays(1))
                .dataFinalizado(LocalDateTime.now())
                .build();
        finalizadoOrder.addItem(OrderItem.builder()
                .id(UUID.randomUUID())
                .product(luva)
                .quantidade(5)
                .precoUnitario(new BigDecimal("10.00"))
                .build());

        lenient().when(authentication.getName()).thenReturn(gerente.getEmail());
        lenient().when(userRepository.findByEmail(gerente.getEmail())).thenReturn(Optional.of(gerente));
        lenient().when(returnRepository.nextNumeroRetornoSequence()).thenReturn(1000L);
        lenient().when(returnRepository.saveAndFlush(any(Return.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(returnRepository.save(any(Return.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_withFinalizadoOrder_returnsCreatedResponse() {
        CreateReturnRequest request = new CreateReturnRequest(
                finalizadoOrder.getId(), List.of(new ReturnItemRequest(luva.getId(), 3)), "Produto com defeito");

        when(orderRepository.findById(finalizadoOrder.getId())).thenReturn(Optional.of(finalizadoOrder));
        when(productRepository.findById(luva.getId())).thenReturn(Optional.of(luva));

        ReturnResponse response = returnService.create(request, authentication);

        assertEquals("DEV-001000", response.numeroRetorno());
        assertEquals("PENDENTE", response.status());
        assertEquals(1, response.items().size());
        assertEquals(3, response.items().get(0).quantidade());
    }

    @Test
    void create_withNonFinalizadoOrder_throwsBadRequestException() {
        finalizadoOrder.setStatus(OrderStatus.SEPARADO);
        CreateReturnRequest request = new CreateReturnRequest(
                finalizadoOrder.getId(), List.of(new ReturnItemRequest(luva.getId(), 1)), null);

        when(orderRepository.findById(finalizadoOrder.getId())).thenReturn(Optional.of(finalizadoOrder));

        assertThrows(BadRequestException.class, () -> returnService.create(request, authentication));
        verify(returnRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_withUnknownOrder_throwsResourceNotFoundException() {
        CreateReturnRequest request = new CreateReturnRequest(
                UUID.randomUUID(), List.of(new ReturnItemRequest(luva.getId(), 1)), null);

        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> returnService.create(request, authentication));
    }

    @Test
    void create_withProductNotInOrder_throwsBadRequestException() {
        Product outro = Product.builder().id(UUID.randomUUID()).nome("Seringa").precoBase(new BigDecimal("5.00")).build();
        CreateReturnRequest request = new CreateReturnRequest(
                finalizadoOrder.getId(), List.of(new ReturnItemRequest(outro.getId(), 1)), null);

        when(orderRepository.findById(finalizadoOrder.getId())).thenReturn(Optional.of(finalizadoOrder));
        when(productRepository.findById(outro.getId())).thenReturn(Optional.of(outro));

        assertThrows(BadRequestException.class, () -> returnService.create(request, authentication));
    }

    @Test
    void create_withQuantityExceedingOrderedAmount_throwsBadRequestException() {
        CreateReturnRequest request = new CreateReturnRequest(
                finalizadoOrder.getId(), List.of(new ReturnItemRequest(luva.getId(), 10)), null);

        when(orderRepository.findById(finalizadoOrder.getId())).thenReturn(Optional.of(finalizadoOrder));
        when(productRepository.findById(luva.getId())).thenReturn(Optional.of(luva));

        assertThrows(BadRequestException.class, () -> returnService.create(request, authentication));
    }

    @Test
    void process_withPendingReturn_incrementsInventoryAndCreatesMovement() {
        Return ret = buildPendingReturn(3);
        Inventory inventory = Inventory.builder().id(UUID.randomUUID()).product(luva).quantidade(10).build();

        when(returnRepository.findById(ret.getId())).thenReturn(Optional.of(ret));
        when(inventoryRepository.findByProductId(luva.getId())).thenReturn(Optional.of(inventory));

        ReturnResponse response = returnService.process(ret.getId(), authentication);

        assertEquals("PROCESSADO", response.status());
        assertEquals(13, inventory.getQuantidade());
        assertNotNull(response.dataProcessamento());
        verify(movementRepository).save(any());
    }

    @Test
    void process_withAlreadyProcessedReturn_throwsBadRequestException() {
        Return ret = buildPendingReturn(3);
        ret.setStatus(ReturnStatus.PROCESSADO);

        when(returnRepository.findById(ret.getId())).thenReturn(Optional.of(ret));

        assertThrows(BadRequestException.class, () -> returnService.process(ret.getId(), authentication));
        verify(inventoryRepository, never()).findByProductId(any());
    }

    @Test
    void process_withUnknownReturn_throwsResourceNotFoundException() {
        when(returnRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> returnService.process(UUID.randomUUID(), authentication));
    }

    private Return buildPendingReturn(int quantidade) {
        Return ret = Return.builder()
                .id(UUID.randomUUID())
                .numeroRetorno("DEV-001000")
                .order(finalizadoOrder)
                .status(ReturnStatus.PENDENTE)
                .dataSolicitacao(LocalDateTime.now())
                .build();
        ret.addItem(ReturnItem.builder()
                .id(UUID.randomUUID())
                .product(luva)
                .quantidade(quantidade)
                .precoUnitario(new BigDecimal("10.00"))
                .build());
        return ret;
    }
}
