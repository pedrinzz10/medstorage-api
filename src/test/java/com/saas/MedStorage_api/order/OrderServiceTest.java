package com.saas.MedStorage_api.order;

import com.saas.MedStorage_api.commission.repository.CommissionRepository;
import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.customer.repository.CustomerRepository;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.InsufficientStockException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.inventory.entity.Inventory;
import com.saas.MedStorage_api.inventory.repository.InventoryRepository;
import com.saas.MedStorage_api.inventorymovement.repository.InventoryMovementRepository;
import com.saas.MedStorage_api.order.dto.CreateOrderRequest;
import com.saas.MedStorage_api.order.dto.OrderItemRequest;
import com.saas.MedStorage_api.order.dto.OrderResponse;
import com.saas.MedStorage_api.order.entity.Order;
import com.saas.MedStorage_api.order.entity.OrderItem;
import com.saas.MedStorage_api.order.enums.OrderStatus;
import com.saas.MedStorage_api.order.repository.OrderRepository;
import com.saas.MedStorage_api.order.service.OrderNotificationService;
import com.saas.MedStorage_api.order.service.OrderService;
import com.saas.MedStorage_api.product.entity.Product;
import com.saas.MedStorage_api.product.repository.ProductRepository;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import com.saas.MedStorage_api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
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
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryMovementRepository movementRepository;
    @Mock
    private CommissionRepository commissionRepository;
    @Mock
    private OrderNotificationService notificationService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private User vendedor;
    private Product luva;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(UUID.randomUUID()).nome("Hospital Central").email("c@h.com").build();
        vendedor = User.builder().id(UUID.randomUUID()).email("vendedor1@distribuidor.com").role(UserRole.VENDEDOR).build();
        luva = Product.builder().id(UUID.randomUUID()).nome("Luva").precoBase(new BigDecimal("10.00")).build();

        lenient().when(authentication.getName()).thenReturn(vendedor.getEmail());
        lenient().when(userRepository.findByEmail(vendedor.getEmail())).thenReturn(Optional.of(vendedor));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(orderRepository.nextNumeroPedidoSequence()).thenReturn(1000L);
    }

    @Test
    void create_withValidRequest_calculatesValorTotal() {
        CreateOrderRequest request = new CreateOrderRequest(
                customer.getId(), List.of(new OrderItemRequest(luva.getId(), 5)), null, null, null);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(productRepository.findById(luva.getId())).thenReturn(Optional.of(luva));

        OrderResponse response = orderService.create(request, authentication);

        assertEquals(new BigDecimal("50.00"), response.valorTotal());
        assertEquals(1, response.items().size());
    }

    /**
     * Analise de valores de fronteira: pedido de 5 unidades a R$10,00 tem
     * valorBruto = 50.00, logo o desconto maximo permitido (50%) e 25.00.
     */
    @ParameterizedTest(name = "desconto de {0} -> permitido: {1}")
    @CsvSource({
            "-0.01, false",  // negativo, nunca permitido
            "0.00,  true",   // sem desconto
            "25.00, true",   // exatamente 50% (limite permitido)
            "25.01, false",  // 1 centavo acima do limite
            "50.00, false"   // desconto igual ao valor total
    })
    void create_validatesMaximumDiscountBoundary(BigDecimal desconto, boolean expectedAllowed) {
        CreateOrderRequest request = new CreateOrderRequest(
                customer.getId(), List.of(new OrderItemRequest(luva.getId(), 5)), desconto, "outro", null);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(productRepository.findById(luva.getId())).thenReturn(Optional.of(luva));

        if (expectedAllowed) {
            OrderResponse response = orderService.create(request, authentication);
            assertEquals(new BigDecimal("50.00").subtract(desconto), response.valorTotal());
        } else {
            assertThrows(BadRequestException.class, () -> orderService.create(request, authentication));
        }
    }

    @Test
    void create_withUnknownCustomer_throwsResourceNotFoundException() {
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(), List.of(new OrderItemRequest(luva.getId(), 1)), null, null, null);

        when(customerRepository.findById(request.customerId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.create(request, authentication));
    }

    @Test
    void create_withUnknownProduct_throwsResourceNotFoundException() {
        CreateOrderRequest request = new CreateOrderRequest(
                customer.getId(), List.of(new OrderItemRequest(UUID.randomUUID(), 1)), null, null, null);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(productRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.create(request, authentication));
    }

    private Order criadoOrderWith(int quantidade) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .numeroPedido("PED-001000")
                .customer(customer)
                .criadoPor(vendedor)
                .status(OrderStatus.CRIADO)
                .valorTotal(new BigDecimal("100.00"))
                .build();
        order.addItem(OrderItem.builder().product(luva).quantidade(quantidade).precoUnitario(luva.getPrecoBase()).build());
        return order;
    }

    @Test
    void changeStatus_toSeparado_withSufficientStock_reservesInventory() {
        Order order = criadoOrderWith(5);
        order.setStatus(OrderStatus.CONFIRMADO);
        Inventory inventory = Inventory.builder().id(UUID.randomUUID()).product(luva).quantidade(100).quantidadeReservada(0).build();

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductId(luva.getId())).thenReturn(Optional.of(inventory));

        OrderResponse response = orderService.changeStatus(order.getId(), "SEPARADO", authentication);

        assertEquals("SEPARADO", response.status());
        assertEquals(100, inventory.getQuantidade());
        assertEquals(5, inventory.getQuantidadeReservada());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void changeStatus_toSeparado_withInsufficientAvailableStock_throwsAndDoesNotReserve() {
        Order order = criadoOrderWith(200);
        order.setStatus(OrderStatus.CONFIRMADO);
        Inventory inventory = Inventory.builder().id(UUID.randomUUID()).product(luva).quantidade(100).quantidadeReservada(0).build();

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductId(luva.getId())).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class,
                () -> orderService.changeStatus(order.getId(), "SEPARADO", authentication));

        assertEquals(OrderStatus.CONFIRMADO, order.getStatus());
        assertEquals(0, inventory.getQuantidadeReservada());
        verify(movementRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void changeStatus_toSeparado_withAlreadySeparadoOrder_throwsBadRequestException() {
        Order order = criadoOrderWith(5);
        order.setStatus(OrderStatus.SEPARADO);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.changeStatus(order.getId(), "SEPARADO", authentication));
        verify(inventoryRepository, never()).findByProductId(any());
    }

    @Test
    void changeStatus_toFinalizado_withProntoOrder_decrementsInventoryAndCreatesMovement() {
        Order order = criadoOrderWith(5);
        order.setStatus(OrderStatus.PRONTO);
        order.setDataConfirmado(java.time.LocalDateTime.now());
        order.setDataSeparado(java.time.LocalDateTime.now());
        order.setDataPronte(java.time.LocalDateTime.now());
        Inventory inventory = Inventory.builder().id(UUID.randomUUID()).product(luva).quantidade(100).quantidadeReservada(5).build();

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductId(luva.getId())).thenReturn(Optional.of(inventory));
        lenient().when(commissionRepository.findByVendedorAndPeriodoInicioAndPeriodoFim(any(), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(commissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.changeStatus(order.getId(), "FINALIZADO", authentication);

        assertEquals("FINALIZADO", response.status());
        assertEquals(95, inventory.getQuantidade());
        assertEquals(0, inventory.getQuantidadeReservada());
        assertNotNull(order.getDataFinalizado());
        verify(movementRepository).save(any());
    }

    @Test
    void changeStatus_toFinalizado_withCriadoOrder_throwsBadRequestException() {
        Order order = criadoOrderWith(5);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.changeStatus(order.getId(), "FINALIZADO", authentication));
        assertEquals(OrderStatus.CRIADO, order.getStatus());
    }

    @Test
    void changeStatus_toCancelado_fromSeparado_releasesReservation() {
        Order order = criadoOrderWith(5);
        order.setStatus(OrderStatus.SEPARADO);
        Inventory inventory = Inventory.builder().id(UUID.randomUUID()).product(luva).quantidade(100).quantidadeReservada(5).build();

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductId(luva.getId())).thenReturn(Optional.of(inventory));

        OrderResponse response = orderService.changeStatus(order.getId(), "CANCELADO", authentication);

        assertEquals("CANCELADO", response.status());
        assertEquals(0, inventory.getQuantidadeReservada());
    }

    @Test
    void changeStatus_toCancelado_fromFinalizado_throwsBadRequestException() {
        Order order = criadoOrderWith(5);
        order.setStatus(OrderStatus.FINALIZADO);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.changeStatus(order.getId(), "CANCELADO", authentication));
    }

    @Test
    void delete_withCriadoOrder_callsRepositoryDelete() {
        Order order = criadoOrderWith(5);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.delete(order.getId());

        verify(orderRepository).delete(order);
    }

    @Test
    void delete_withConfirmadoOrder_throwsBadRequestException() {
        Order order = criadoOrderWith(5);
        order.setStatus(OrderStatus.CONFIRMADO);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> orderService.delete(order.getId()));
        verify(orderRepository, never()).delete(any(Order.class));
    }

    @Test
    void delete_withUnknownId_throwsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.delete(unknownId));
    }

    @Test
    void update_withCriadoOrder_replacesItemsAndRecalculatesTotal() {
        Order order = criadoOrderWith(5);
        Product seringa = Product.builder().id(UUID.randomUUID()).nome("Seringa").precoBase(new BigDecimal("5.00")).build();
        CreateOrderRequest request = new CreateOrderRequest(
                customer.getId(), List.of(new OrderItemRequest(seringa.getId(), 3)), null, null, "obs");

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(productRepository.findById(seringa.getId())).thenReturn(Optional.of(seringa));

        OrderResponse response = orderService.update(order.getId(), request, authentication);

        assertEquals(new BigDecimal("15.00"), response.valorTotal());
        assertEquals(1, response.items().size());
        verify(orderRepository).saveAndFlush(any(Order.class));
    }

    @Test
    void update_withConfirmadoOrder_throwsBadRequestException() {
        Order order = criadoOrderWith(5);
        order.setStatus(OrderStatus.CONFIRMADO);
        CreateOrderRequest request = new CreateOrderRequest(
                customer.getId(), List.of(new OrderItemRequest(luva.getId(), 1)), null, null, null);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> orderService.update(order.getId(), request, authentication));
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    @Test
    void changeStatus_toConfirmado_withCriadoOrder_setsDataConfirmado() {
        Order order = criadoOrderWith(5);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = orderService.changeStatus(order.getId(), "CONFIRMADO", authentication);

        assertEquals("CONFIRMADO", response.status());
        assertNotNull(order.getDataConfirmado());
    }

    @Test
    void changeStatus_toConfirmado_withNonCriadoOrder_throwsBadRequestException() {
        Order order = criadoOrderWith(5);
        order.setStatus(OrderStatus.SEPARADO);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.changeStatus(order.getId(), "CONFIRMADO", authentication));
        assertEquals(OrderStatus.SEPARADO, order.getStatus());
    }

    @Test
    void changeStatus_toPronte_withSeparadoOrder_setsDataPronte() {
        Order order = criadoOrderWith(5);
        order.setStatus(OrderStatus.SEPARADO);
        order.setDataConfirmado(java.time.LocalDateTime.now());
        order.setDataSeparado(java.time.LocalDateTime.now());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = orderService.changeStatus(order.getId(), "PRONTO", authentication);

        assertEquals("PRONTO", response.status());
        assertNotNull(order.getDataPronte());
    }

    @Test
    void changeStatus_toPronte_withNonSeparadoOrder_throwsBadRequestException() {
        Order order = criadoOrderWith(5);
        // CRIADO → PRONTO sem passar por CONFIRMADO/SEPARADO deve falhar
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.changeStatus(order.getId(), "PRONTO", authentication));
        assertEquals(OrderStatus.CRIADO, order.getStatus());
    }

    @Test
    void changeStatus_toCancelado_fromPronte_releasesReservation() {
        Order order = criadoOrderWith(5);
        order.setStatus(OrderStatus.PRONTO);
        Inventory inventory = Inventory.builder()
                .id(UUID.randomUUID()).product(luva).quantidade(100).quantidadeReservada(5).build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductId(luva.getId())).thenReturn(Optional.of(inventory));

        OrderResponse response = orderService.changeStatus(order.getId(), "CANCELADO", authentication);

        assertEquals("CANCELADO", response.status());
        assertEquals(0, inventory.getQuantidadeReservada());
    }

    @Test
    void changeStatus_toCancelado_fromCriado_doesNotTouchInventory() {
        Order order = criadoOrderWith(5);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = orderService.changeStatus(order.getId(), "CANCELADO", authentication);

        assertEquals("CANCELADO", response.status());
        verify(inventoryRepository, never()).findByProductId(any());
    }

    @Test
    void changeStatus_withUnsupportedStatus_throwsBadRequestException() {
        Order order = criadoOrderWith(5);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.changeStatus(order.getId(), "INVALIDO", authentication));
    }
}
