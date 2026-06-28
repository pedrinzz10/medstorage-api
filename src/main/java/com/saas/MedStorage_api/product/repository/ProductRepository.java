package com.saas.MedStorage_api.product.repository;

import com.saas.MedStorage_api.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByAtivoTrue(Pageable pageable);

    boolean existsBySku(String sku);
}
