package com.saas.MedStorage_api.product.repository;

import com.saas.MedStorage_api.product.entity.ProductSalesView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductSalesViewRepository extends JpaRepository<ProductSalesView, UUID> {
}
