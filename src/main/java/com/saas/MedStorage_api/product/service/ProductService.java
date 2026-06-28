package com.saas.MedStorage_api.product.service;

import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.inventory.entity.Inventory;
import com.saas.MedStorage_api.inventory.repository.InventoryRepository;
import com.saas.MedStorage_api.product.dto.ProductRequest;
import com.saas.MedStorage_api.product.dto.ProductResponse;
import com.saas.MedStorage_api.product.entity.Product;
import com.saas.MedStorage_api.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public ProductService(ProductRepository productRepository, InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findByAtivoTrue(pageable).map(ProductResponse::from);
    }

    public ProductResponse findById(UUID id) {
        return ProductResponse.from(getOrThrow(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new BadRequestException("SKU already in use: " + request.sku());
        }
        Product product = productRepository.save(Product.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .sku(request.sku())
                .precoBase(request.precoBase())
                .unidade(request.unidade())
                .estoqueMinimo(request.estoqueMinimo())
                .ativo(request.ativo())
                .build());
        inventoryRepository.save(Inventory.builder()
                .product(product)
                .quantidade(0)
                .build());
        log.info("Produto {} criado com SKU {}", product.getNome(), product.getSku());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = getOrThrow(id);
        if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())) {
            throw new BadRequestException("SKU already in use: " + request.sku());
        }
        product.setNome(request.nome());
        product.setDescricao(request.descricao());
        product.setSku(request.sku());
        product.setPrecoBase(request.precoBase());
        product.setUnidade(request.unidade());
        product.setEstoqueMinimo(request.estoqueMinimo());
        product.setAtivo(request.ativo());
        log.info("Produto {} atualizado", product.getSku());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void deactivate(UUID id) {
        Product product = getOrThrow(id);
        product.setAtivo(false);
        productRepository.save(product);
        log.info("Produto {} desativado", product.getSku());
    }

    private Product getOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }
}
