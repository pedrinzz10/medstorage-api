package com.saas.MedStorage_api.consignment.service;

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
import com.saas.MedStorage_api.consignment.entity.ConsignmentUsage;
import com.saas.MedStorage_api.consignment.enums.ConsignmentStatus;
import com.saas.MedStorage_api.consignment.repository.ConsignmentItemRepository;
import com.saas.MedStorage_api.consignment.repository.ConsignmentRepository;
import com.saas.MedStorage_api.consignment.repository.ConsignmentUsageRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ConsignmentService {

    private final ConsignmentRepository consignmentRepository;
    private final ConsignmentItemRepository consignmentItemRepository;
    private final ConsignmentUsageRepository consignmentUsageRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductBatchRepository productBatchRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final UserRepository userRepository;

    public ConsignmentService(
            ConsignmentRepository consignmentRepository,
            ConsignmentItemRepository consignmentItemRepository,
            ConsignmentUsageRepository consignmentUsageRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            ProductBatchRepository productBatchRepository,
            InventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository,
            UserRepository userRepository) {
        this.consignmentRepository = consignmentRepository;
        this.consignmentItemRepository = consignmentItemRepository;
        this.consignmentUsageRepository = consignmentUsageRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.productBatchRepository = productBatchRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ConsignmentResponse create(CreateConsignmentRequest request, Authentication authentication) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        User criadoPor = currentUser(authentication);

        Consignment consignment = Consignment.builder()
                .customer(customer)
                .criadoPor(criadoPor)
                .observacoes(request.observacoes())
                .build();
        Consignment saved = consignmentRepository.saveAndFlush(consignment);

        for (ConsignmentItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));

            Inventory inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product " + product.getNome()));

            int disponivel = inventory.getQuantidade() - inventory.getQuantidadeReservada();
            if (disponivel < itemRequest.quantidade()) {
                throw new InsufficientStockException(
                        "Insufficient available stock for product " + product.getNome()
                        + ". Available: " + disponivel + ", requested: " + itemRequest.quantidade());
            }

            ProductBatch batch = pickBatch(product.getId(), itemRequest.quantidade());

            if (batch != null) {
                batch.setQuantidade(batch.getQuantidade() - itemRequest.quantidade());
                productBatchRepository.save(batch);
            }

            inventory.setQuantidade(inventory.getQuantidade() - itemRequest.quantidade());
            inventoryRepository.save(inventory);

            movementRepository.save(InventoryMovement.builder()
                    .product(product)
                    .tipo(MovementType.OUT)
                    .quantidade(itemRequest.quantidade())
                    .motivo("Consignação — " + customer.getNome())
                    .referenciaId(saved.getId())
                    .referenciaTipo("CONSIGNMENT")
                    .criadoPor(criadoPor)
                    .build());

            saved.addItem(ConsignmentItem.builder()
                    .product(product)
                    .batch(batch)
                    .quantidadeEnviada(itemRequest.quantidade())
                    .precoUnitario(product.getPrecoBase())
                    .build());
        }

        Consignment persisted = consignmentRepository.save(saved);
        log.info("Consignação criada para cliente {} com {} item(ns) por user={}",
                customer.getNome(), request.items().size(), authentication.getName());
        return ConsignmentResponse.from(persisted);
    }

    /**
     * Um único lote precisa cobrir a quantidade inteira (sem rateio, diferente
     * do FEFO de pedidos) — quantidades de consignação tendem a ser pequenas.
     * Produto sem nenhum lote cadastrado (estoque legado) não participa do
     * controle por lote, mantendo compatibilidade.
     */
    private ProductBatch pickBatch(UUID productId, int quantidade) {
        List<ProductBatch> lotes = productBatchRepository.findByProductIdOrderByValidadeAsc(productId);
        if (lotes.isEmpty()) {
            return null;
        }
        return lotes.stream()
                .filter(b -> b.getQuantidade() >= quantidade)
                .findFirst()
                .orElseThrow(() -> new InsufficientStockException(
                        "Nenhum lote com quantidade suficiente para atender " + quantidade + " unidade(s)"));
    }

    @Transactional(readOnly = true)
    public ConsignmentResponse findById(UUID id) {
        return ConsignmentResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ConsignmentResponse> findAll(UUID customerId, ConsignmentStatus status, Pageable pageable) {
        Page<Consignment> page;
        if (customerId != null && status != null) {
            page = consignmentRepository.findByCustomer_IdAndStatus(customerId, status, pageable);
        } else if (customerId != null) {
            page = consignmentRepository.findByCustomer_Id(customerId, pageable);
        } else if (status != null) {
            page = consignmentRepository.findByStatus(status, pageable);
        } else {
            page = consignmentRepository.findAll(pageable);
        }
        return page.map(ConsignmentResponse::from);
    }

    @Transactional
    public ConsignmentUsageResponse registerUsage(UUID consignmentId, UUID itemId, RegisterUsageRequest request, Authentication authentication) {
        ConsignmentItem item = getItemOrThrow(consignmentId, itemId);
        if (request.quantidade() > item.getSaldoDisponivel()) {
            throw new BadRequestException(
                    "Quantidade excede o saldo disponível no cliente. Disponível: " + item.getSaldoDisponivel());
        }

        User criadoPor = currentUser(authentication);
        BigDecimal valorFaturado = item.getPrecoUnitario().multiply(BigDecimal.valueOf(request.quantidade()));

        item.setQuantidadeUsada(item.getQuantidadeUsada() + request.quantidade());
        consignmentItemRepository.save(item);

        ConsignmentUsage usage = ConsignmentUsage.builder()
                .consignmentItem(item)
                .quantidade(request.quantidade())
                .valorFaturado(valorFaturado)
                .dataUso(request.dataUso())
                .criadoPor(criadoPor)
                .build();
        ConsignmentUsage saved = consignmentUsageRepository.save(usage);

        checkAutoClose(item.getConsignment());
        log.info("Uso registrado: item={} quantidade={} valor={}", item.getId(), request.quantidade(), valorFaturado);
        return ConsignmentUsageResponse.from(saved);
    }

    @Transactional
    public ConsignmentItemResponse registerReturn(UUID consignmentId, UUID itemId, RegisterReturnRequest request, Authentication authentication) {
        ConsignmentItem item = getItemOrThrow(consignmentId, itemId);
        if (request.quantidade() > item.getSaldoDisponivel()) {
            throw new BadRequestException(
                    "Quantidade excede o saldo disponível no cliente. Disponível: " + item.getSaldoDisponivel());
        }

        User criadoPor = currentUser(authentication);
        item.setQuantidadeDevolvida(item.getQuantidadeDevolvida() + request.quantidade());
        consignmentItemRepository.save(item);

        if (item.getBatch() != null) {
            ProductBatch batch = item.getBatch();
            batch.setQuantidade(batch.getQuantidade() + request.quantidade());
            productBatchRepository.save(batch);
        }

        Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product " + item.getProduct().getNome()));
        inventory.setQuantidade(inventory.getQuantidade() + request.quantidade());
        inventoryRepository.save(inventory);

        movementRepository.save(InventoryMovement.builder()
                .product(item.getProduct())
                .tipo(MovementType.IN)
                .quantidade(request.quantidade())
                .motivo("Devolução de consignação — " + item.getConsignment().getCustomer().getNome())
                .referenciaId(item.getConsignment().getId())
                .referenciaTipo("CONSIGNMENT_RETURN")
                .criadoPor(criadoPor)
                .build());

        checkAutoClose(item.getConsignment());
        log.info("Devolução registrada: item={} quantidade={}", item.getId(), request.quantidade());
        return ConsignmentItemResponse.from(item);
    }

    @Transactional
    public ConsignmentResponse close(UUID consignmentId) {
        Consignment consignment = getOrThrow(consignmentId);
        consignment.setStatus(ConsignmentStatus.ENCERRADO);
        Consignment saved = consignmentRepository.save(consignment);
        log.info("Consignação {} encerrada manualmente", consignmentId);
        return ConsignmentResponse.from(saved);
    }

    private void checkAutoClose(Consignment consignment) {
        boolean tudoQuitado = consignment.getItems().stream().allMatch(i -> i.getSaldoDisponivel() == 0);
        if (tudoQuitado && consignment.getStatus() == ConsignmentStatus.ATIVO) {
            consignment.setStatus(ConsignmentStatus.ENCERRADO);
            consignmentRepository.save(consignment);
            log.info("Consignação {} encerrada automaticamente (saldo zerado)", consignment.getId());
        }
    }

    private ConsignmentItem getItemOrThrow(UUID consignmentId, UUID itemId) {
        ConsignmentItem item = consignmentItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Consignment item not found"));
        if (!item.getConsignment().getId().equals(consignmentId)) {
            throw new ResourceNotFoundException("Consignment item not found");
        }
        return item;
    }

    private Consignment getOrThrow(UUID id) {
        return consignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consignment not found"));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
