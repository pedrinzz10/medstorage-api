package com.saas.MedStorage_api.returns.service;

import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.inventory.entity.Inventory;
import com.saas.MedStorage_api.inventory.repository.InventoryRepository;
import com.saas.MedStorage_api.inventorymovement.entity.InventoryMovement;
import com.saas.MedStorage_api.inventorymovement.enums.MovementType;
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
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class ReturnService {

    private final ReturnRepository returnRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;

    public ReturnService(
            ReturnRepository returnRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            InventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository) {
        this.returnRepository = returnRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public ReturnResponse create(CreateReturnRequest request, Authentication authentication) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.RETIRADO) {
            throw new BadRequestException(
                    "Returns are only allowed for orders with status RETIRADO, current status: " + order.getStatus());
        }

        Return ret = Return.builder()
                .numeroRetorno(generateNumeroRetorno())
                .order(order)
                .status(ReturnStatus.PENDENTE)
                .motivo(request.motivo())
                .dataSolicitacao(LocalDateTime.now())
                .build();

        for (ReturnItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));

            OrderItem orderItem = order.getItems().stream()
                    .filter(oi -> oi.getProduct().getId().equals(itemRequest.productId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                            "Product " + product.getNome() + " was not part of order " + order.getNumeroPedido()));

            if (itemRequest.quantidade() > orderItem.getQuantidade()) {
                throw new BadRequestException(
                        "Return quantity for " + product.getNome()
                        + " (" + itemRequest.quantidade() + ") exceeds ordered quantity (" + orderItem.getQuantidade() + ")");
            }

            ret.addItem(ReturnItem.builder()
                    .product(product)
                    .quantidade(itemRequest.quantidade())
                    .precoUnitario(orderItem.getPrecoUnitario())
                    .build());
        }

        ReturnResponse response = ReturnResponse.from(returnRepository.saveAndFlush(ret));
        log.info("Devolução {} criada para pedido {}", response.numeroRetorno(), order.getNumeroPedido());
        return response;
    }

    public ReturnResponse findById(UUID id) {
        return ReturnResponse.from(getOrThrow(id));
    }

    public Page<ReturnResponse> findAll(Pageable pageable) {
        return returnRepository.findAll(pageable).map(ReturnResponse::from);
    }

    @Transactional
    public ReturnResponse process(UUID id, Authentication authentication) {
        Return ret = getOrThrow(id);

        if (ret.getStatus() != ReturnStatus.PENDENTE) {
            throw new BadRequestException(
                    "Return cannot be processed: current status is " + ret.getStatus());
        }

        User actingUser = currentUser(authentication);

        for (ReturnItem item : ret.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for product " + item.getProduct().getNome()));

            inventory.setQuantidade(inventory.getQuantidade() + item.getQuantidade());
            inventoryRepository.save(inventory);

            movementRepository.save(InventoryMovement.builder()
                    .product(item.getProduct())
                    .tipo(MovementType.IN)
                    .quantidade(item.getQuantidade())
                    .motivo("Devolução Pedido " + ret.getOrder().getNumeroPedido())
                    .referenciaId(ret.getId())
                    .referenciaTipo("return")
                    .criadoPor(actingUser)
                    .build());
        }

        ret.setStatus(ReturnStatus.PROCESSADO);
        ret.setDataProcessamento(LocalDateTime.now());
        ret.setProcessadoPor(actingUser);

        Return saved = returnRepository.save(ret);
        log.info("Devolução {} processada por user={}", saved.getNumeroRetorno(), authentication.getName());
        return ReturnResponse.from(saved);
    }

    @Transactional
    public ReturnResponse reject(UUID id, Authentication authentication) {
        Return ret = getOrThrow(id);

        if (ret.getStatus() != ReturnStatus.PENDENTE) {
            throw new BadRequestException(
                    "Return cannot be rejected: current status is " + ret.getStatus());
        }

        ret.setStatus(ReturnStatus.REJEITADO);
        ret.setDataProcessamento(LocalDateTime.now());
        ret.setProcessadoPor(currentUser(authentication));

        Return saved = returnRepository.save(ret);
        log.info("Devolução {} rejeitada por user={}", saved.getNumeroRetorno(), authentication.getName());
        return ReturnResponse.from(saved);
    }

    private String generateNumeroRetorno() {
        long sequence = returnRepository.nextNumeroRetornoSequence();
        return "DEV-" + String.format("%06d", sequence);
    }

    private Return getOrThrow(UUID id) {
        return returnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
