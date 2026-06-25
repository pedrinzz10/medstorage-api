package com.saas.MedStorage_api.order;

import com.saas.MedStorage_api.customer.Customer;
import com.saas.MedStorage_api.customer.CustomerRepository;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.InsufficientStockException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.inventory.Inventory;
import com.saas.MedStorage_api.inventory.InventoryRepository;
import com.saas.MedStorage_api.inventorymovement.InventoryMovement;
import com.saas.MedStorage_api.inventorymovement.InventoryMovementRepository;
import com.saas.MedStorage_api.inventorymovement.MovementType;
import com.saas.MedStorage_api.product.Product;
import com.saas.MedStorage_api.product.ProductRepository;
import com.saas.MedStorage_api.user.User;
import com.saas.MedStorage_api.user.UserRepository;
import com.saas.MedStorage_api.order.dto.CreateOrderRequest;
import com.saas.MedStorage_api.order.dto.OrderItemRequest;
import com.saas.MedStorage_api.order.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {

    private static final BigDecimal MAX_DISCOUNT_RATIO = new BigDecimal("0.5");

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final OrderNotificationService notificationService;

    public OrderService(
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            InventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository,
            OrderNotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, Authentication authentication) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        User criadoPor = currentUser(authentication);

        Order order = Order.builder()
                .numeroPedido(generateNumeroPedido())
                .customer(customer)
                .criadoPor(criadoPor)
                .status(OrderStatus.PENDENTE)
                .tipoDesconto(request.tipoDesconto())
                .notas(request.notas())
                .build();

        BigDecimal valorBruto = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));

            BigDecimal subtotal = product.getPrecoBase().multiply(BigDecimal.valueOf(itemRequest.quantidade()));
            valorBruto = valorBruto.add(subtotal);

            order.addItem(OrderItem.builder()
                    .product(product)
                    .quantidade(itemRequest.quantidade())
                    .precoUnitario(product.getPrecoBase())
                    .build());
        }

        BigDecimal desconto = request.descontoAplicado() == null ? BigDecimal.ZERO : request.descontoAplicado();
        validateDiscount(desconto, valorBruto);

        order.setDescontoAplicado(desconto);
        order.setValorTotal(valorBruto.subtract(desconto));

        // saveAndFlush forca a execucao do INSERT agora, para que o Hibernate
        // recarregue numero_pedido (gerado pelo trigger no banco, ver @Generated
        // no Order) antes de montar a resposta.
        return OrderResponse.from(orderRepository.saveAndFlush(order));
    }

    public OrderResponse findById(UUID id) {
        return OrderResponse.from(getOrThrow(id));
    }

    public Page<OrderResponse> findAll(
            OrderStatus status,
            UUID customerId,
            UUID criadoPor,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            BigDecimal valorMin,
            BigDecimal valorMax,
            Pageable pageable) {
        return orderRepository
                .findAll(OrderSpecifications.withFilters(
                        status, customerId, criadoPor, dataInicio, dataFim, valorMin, valorMax), pageable)
                .map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse markAsAttended(UUID orderId, Authentication authentication) {
        Order order = getOrThrow(orderId);

        if (order.getStatus() != OrderStatus.PENDENTE) {
            throw new BadRequestException("Order cannot transition from " + order.getStatus() + " to ATENDIDO");
        }

        User actingUser = currentUser(authentication);

        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for product " + item.getProduct().getNome()));

            if (inventory.getQuantidade() < item.getQuantidade()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product " + item.getProduct().getNome());
            }

            inventory.setQuantidade(inventory.getQuantidade() - item.getQuantidade());
            inventoryRepository.save(inventory);

            movementRepository.save(InventoryMovement.builder()
                    .product(item.getProduct())
                    .tipo(MovementType.OUT)
                    .quantidade(item.getQuantidade())
                    .motivo("Pedido " + order.getNumeroPedido())
                    .referenciaId(order.getId())
                    .referenciaTipo("order")
                    .criadoPor(actingUser)
                    .build());
        }

        order.setStatus(OrderStatus.ATENDIDO);
        order.setDataAtendimento(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        registerNotificationAfterCommit(saved);

        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse markAsWithdrawn(UUID orderId) {
        Order order = getOrThrow(orderId);

        if (order.getStatus() != OrderStatus.ATENDIDO) {
            throw new BadRequestException("Order cannot transition from " + order.getStatus() + " to RETIRADO");
        }

        order.setStatus(OrderStatus.RETIRADO);
        order.setDataRetirada(LocalDateTime.now());

        return OrderResponse.from(orderRepository.save(order));
    }

    private void validateDiscount(BigDecimal desconto, BigDecimal valorBruto) {
        if (desconto.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Discount cannot be negative");
        }
        BigDecimal maxDesconto = valorBruto.multiply(MAX_DISCOUNT_RATIO).setScale(2, RoundingMode.HALF_UP);
        if (desconto.compareTo(maxDesconto) > 0) {
            throw new BadRequestException("Discount cannot exceed 50% of the order value");
        }
    }

    private User currentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    /**
     * Gera o numero a partir da mesma sequence usada pelo trigger
     * generate_order_number() (mantido no banco como rede de seguranca para
     * inserts diretos via SQL). Gerar aqui em Java evita depender do
     * Hibernate reler o valor setado pelo trigger apos o INSERT, o que se
     * mostrou nao-deterministico dentro de transacoes de teste aninhadas.
     */
    private String generateNumeroPedido() {
        long sequence = orderRepository.nextNumeroPedidoSequence();
        return "PED-" + String.format("%06d", sequence);
    }

    private Order getOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private void registerNotificationAfterCommit(Order order) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Sem transacao Spring ativa (ex.: chamada direta em teste unitario) -
            // nao ha commit para esperar, envia imediatamente.
            notificationService.sendOrderReadyEmail(order);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.sendOrderReadyEmail(order);
            }
        });
    }
}
