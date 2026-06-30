package com.saas.MedStorage_api.order.service;

import com.saas.MedStorage_api.commission.entity.Commission;
import com.saas.MedStorage_api.commission.repository.CommissionRepository;
import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.customer.repository.CustomerRepository;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.InsufficientStockException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.inventory.entity.Inventory;
import com.saas.MedStorage_api.inventory.repository.InventoryRepository;
import com.saas.MedStorage_api.inventorymovement.entity.InventoryMovement;
import com.saas.MedStorage_api.inventorymovement.enums.MovementType;
import com.saas.MedStorage_api.inventorymovement.repository.InventoryMovementRepository;
import com.saas.MedStorage_api.order.dto.CreateOrderRequest;
import com.saas.MedStorage_api.order.dto.OrderItemRequest;
import com.saas.MedStorage_api.order.dto.OrderResponse;
import com.saas.MedStorage_api.order.entity.Order;
import com.saas.MedStorage_api.order.entity.OrderItem;
import com.saas.MedStorage_api.order.enums.OrderStatus;
import com.saas.MedStorage_api.order.repository.OrderRepository;
import com.saas.MedStorage_api.order.repository.OrderSpecifications;
import com.saas.MedStorage_api.product.entity.Product;
import com.saas.MedStorage_api.product.repository.ProductRepository;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class OrderService {

    private static final BigDecimal MAX_DISCOUNT_RATIO = new BigDecimal("0.5");

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final CommissionRepository commissionRepository;
    private final OrderNotificationService notificationService;

    public OrderService(
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            InventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository,
            CommissionRepository commissionRepository,
            OrderNotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.commissionRepository = commissionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, Authentication authentication) {
        log.info("Criando pedido: customer={} itens={} user={}", request.customerId(), request.items().size(), authentication.getName());
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        User criadoPor = currentUser(authentication);

        Order order = Order.builder()
                .numeroPedido(generateNumeroPedido())
                .customer(customer)
                .criadoPor(criadoPor)
                .status(OrderStatus.CRIADO)
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
        Order saved = orderRepository.saveAndFlush(order);
        registerAfterCommit(() -> notificationService.sendOrderCreatedToStaff(saved));
        OrderResponse response = OrderResponse.from(saved);
        log.info("Pedido {} criado: valor={}", response.numeroPedido(), response.valorTotal());
        return response;
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
    public void delete(UUID id) {
        Order order = getOrThrow(id);
        if (order.getStatus() != OrderStatus.CRIADO) {
            throw new BadRequestException("Only CRIADO orders can be deleted");
        }
        orderRepository.delete(order);
        log.info("Pedido {} excluido", order.getNumeroPedido());
    }

    @Transactional
    public OrderResponse update(UUID id, CreateOrderRequest request, Authentication authentication) {
        Order order = getOrThrow(id);
        if (order.getStatus() != OrderStatus.CRIADO) {
            throw new BadRequestException("Only CRIADO orders can be updated");
        }

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        order.setCustomer(customer);
        order.setTipoDesconto(request.tipoDesconto());
        order.setNotas(request.notas());

        order.getItems().clear();

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

        Order saved = orderRepository.saveAndFlush(order);
        log.info("Pedido {} atualizado por user={}", saved.getNumeroPedido(), authentication.getName());
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse changeStatus(UUID orderId, String newStatus, Authentication authentication) {
        Order order = getOrThrow(orderId);
        User actingUser = currentUser(authentication);
        return switch (newStatus.toUpperCase()) {
            case "CONFIRMADO" -> markAsConfirmado(order, actingUser);
            case "SEPARADO"   -> markAsSeparado(order, actingUser);
            case "PRONTO"     -> markAsPronte(order, actingUser);
            case "FINALIZADO" -> markAsFinalizado(order, actingUser);
            case "CANCELADO"  -> markAsCancelado(order, actingUser);
            default -> throw new BadRequestException("Status transition to '" + newStatus + "' is not supported");
        };
    }

    private OrderResponse markAsConfirmado(Order order, User actingUser) {
        if (order.getStatus() != OrderStatus.CRIADO) {
            throw new BadRequestException("Order cannot transition from " + order.getStatus() + " to CONFIRMADO");
        }
        order.setStatus(OrderStatus.CONFIRMADO);
        order.setDataConfirmado(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info("Pedido {} marcado como CONFIRMADO por user={}", saved.getNumeroPedido(), actingUser.getEmail());
        return OrderResponse.from(saved);
    }

    private OrderResponse markAsSeparado(Order order, User actingUser) {
        if (order.getStatus() != OrderStatus.CONFIRMADO) {
            throw new BadRequestException("Order cannot transition from " + order.getStatus() + " to SEPARADO");
        }
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for product " + item.getProduct().getNome()));

            int disponivel = inventory.getQuantidade() - inventory.getQuantidadeReservada();
            if (disponivel < item.getQuantidade()) {
                throw new InsufficientStockException(
                        "Insufficient available stock for product " + item.getProduct().getNome()
                        + ". Available: " + disponivel + ", requested: " + item.getQuantidade());
            }
            inventory.setQuantidadeReservada(inventory.getQuantidadeReservada() + item.getQuantidade());
            inventoryRepository.save(inventory);
        }
        order.setStatus(OrderStatus.SEPARADO);
        order.setDataSeparado(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info("Pedido {} marcado como SEPARADO por user={}", saved.getNumeroPedido(), actingUser.getEmail());
        return OrderResponse.from(saved);
    }

    private OrderResponse markAsPronte(Order order, User actingUser) {
        if (order.getStatus() != OrderStatus.SEPARADO) {
            throw new BadRequestException("Order cannot transition from " + order.getStatus() + " to PRONTO");
        }
        order.setStatus(OrderStatus.PRONTO);
        order.setDataPronte(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info("Pedido {} marcado como PRONTO por user={}", saved.getNumeroPedido(), actingUser.getEmail());
        registerAfterCommit(() -> notificationService.sendOrderReadyEmail(saved));
        registerAfterCommit(() -> notificationService.sendOrderAttendedToStaff(saved));
        return OrderResponse.from(saved);
    }

    private OrderResponse markAsFinalizado(Order order, User actingUser) {
        if (order.getStatus() != OrderStatus.PRONTO) {
            throw new BadRequestException("Order cannot transition from " + order.getStatus() + " to FINALIZADO");
        }
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for product " + item.getProduct().getNome()));

            inventory.setQuantidade(inventory.getQuantidade() - item.getQuantidade());
            inventory.setQuantidadeReservada(inventory.getQuantidadeReservada() - item.getQuantidade());
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
        order.setStatus(OrderStatus.FINALIZADO);
        order.setDataFinalizado(LocalDateTime.now());
        order.setFinalizadoPor(actingUser);
        Order saved = orderRepository.save(order);
        log.info("Pedido {} marcado como FINALIZADO por user={}", saved.getNumeroPedido(), actingUser.getEmail());
        acumulaComissao(saved);
        return OrderResponse.from(saved);
    }

    private OrderResponse markAsCancelado(Order order, User actingUser) {
        if (order.getStatus() == OrderStatus.FINALIZADO) {
            throw new BadRequestException("Orders with status FINALIZADO cannot be cancelled");
        }
        if (order.getStatus() == OrderStatus.SEPARADO || order.getStatus() == OrderStatus.PRONTO) {
            liberarReserva(order);
        }
        order.setStatus(OrderStatus.CANCELADO);
        Order saved = orderRepository.save(order);
        log.info("Pedido {} marcado como CANCELADO por user={}", saved.getNumeroPedido(), actingUser.getEmail());
        return OrderResponse.from(saved);
    }

    private void liberarReserva(Order order) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for product " + item.getProduct().getNome()));
            inventory.setQuantidadeReservada(
                    Math.max(0, inventory.getQuantidadeReservada() - item.getQuantidade()));
            inventoryRepository.save(inventory);
        }
    }

    private void acumulaComissao(Order order) {
        User vendedor = order.getCriadoPor();
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
        int totalUnidades = order.getItems().stream().mapToInt(OrderItem::getQuantidade).sum();

        Commission commission = commissionRepository
                .findByVendedorAndPeriodoInicioAndPeriodoFim(vendedor, inicio, fim)
                .orElseGet(() -> Commission.builder()
                        .vendedor(vendedor)
                        .periodoInicio(inicio)
                        .periodoFim(fim)
                        .build());

        commission.setTotalPedidos(commission.getTotalPedidos() + 1);
        commission.setValorVendido(commission.getValorVendido().add(order.getValorTotal()));
        commission.setQuantidadeUnidades(commission.getQuantidadeUnidades() + totalUnidades);

        commissionRepository.save(commission);
        log.debug("Comissao acumulada: vendedor={} periodo={}/{} valorVendido={}",
                vendedor.getEmail(), inicio, fim, commission.getValorVendido());
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

    private void registerAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
