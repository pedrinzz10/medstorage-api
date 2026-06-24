package com.saas.MedStorage_api.order;

import com.saas.MedStorage_api.domain.customer.Customer;
import com.saas.MedStorage_api.domain.customer.CustomerRepository;
import com.saas.MedStorage_api.domain.inventory.Inventory;
import com.saas.MedStorage_api.domain.inventory.InventoryRepository;
import com.saas.MedStorage_api.domain.inventorymovement.InventoryMovementRepository;
import com.saas.MedStorage_api.domain.order.Order;
import com.saas.MedStorage_api.domain.order.OrderItem;
import com.saas.MedStorage_api.domain.order.OrderRepository;
import com.saas.MedStorage_api.domain.order.OrderStatus;
import com.saas.MedStorage_api.domain.product.Product;
import com.saas.MedStorage_api.domain.product.ProductRepository;
import com.saas.MedStorage_api.domain.user.User;
import com.saas.MedStorage_api.domain.user.UserRepository;
import com.saas.MedStorage_api.domain.user.UserRole;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.InsufficientStockException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.notification.OrderNotificationService;
import com.saas.MedStorage_api.order.dto.CreateOrderRequest;
import com.saas.MedStorage_api.order.dto.OrderItemRequest;
import com.saas.MedStorage_api.order.dto.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
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

    @Test
    void create_withDiscountAboveFiftyPercent_throwsBadRequestException() {
        CreateOrderRequest request = new CreateOrderRequest(
                customer.getId(), List.of(new OrderItemRequest(luva.getId(), 5)), new BigDecimal("30.00"), "outro", null);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(productRepository.findById(luva.getId())).thenReturn(Optional.of(luva));

        assertThrows(BadRequestException.class, () -> orderService.create(request, authentication));
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

    private Order pendingOrderWith(int quantidade) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .numeroPedido("PED-001000")
                .customer(customer)
                .criadoPor(vendedor)
                .status(OrderStatus.PENDENTE)
                .valorTotal(new BigDecimal("100.00"))
                .build();
        order.addItem(OrderItem.builder().product(luva).quantidade(quantidade).precoUnitario(luva.getPrecoBase()).build());
        return order;
    }

    @Test
    void markAsAttended_withSufficientStock_decrementsInventoryAndCreatesMovement() {
        Order order = pendingOrderWith(5);
        Inventory inventory = Inventory.builder().id(UUID.randomUUID()).product(luva).quantidade(100).build();

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductId(luva.getId())).thenReturn(Optional.of(inventory));

        OrderResponse response = orderService.markAsAttended(order.getId(), authentication);

        assertEquals("ATENDIDO", response.status());
        assertEquals(95, inventory.getQuantidade());
        verify(movementRepository).save(any());
    }

    @Test
    void markAsAttended_withInsufficientStock_throwsAndDoesNotChangeStatus() {
        Order order = pendingOrderWith(200);
        Inventory inventory = Inventory.builder().id(UUID.randomUUID()).product(luva).quantidade(100).build();

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductId(luva.getId())).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class, () -> orderService.markAsAttended(order.getId(), authentication));

        assertEquals(OrderStatus.PENDENTE, order.getStatus());
        assertEquals(100, inventory.getQuantidade());
        verify(movementRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void markAsAttended_withAlreadyAttendedOrder_throwsBadRequestException() {
        Order order = pendingOrderWith(5);
        order.setStatus(OrderStatus.ATENDIDO);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> orderService.markAsAttended(order.getId(), authentication));
        verify(inventoryRepository, never()).findByProductId(any());
    }
}
