package com.saas.MedStorage_api.seller.repository;

import com.saas.MedStorage_api.seller.entity.SellerPerformanceView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SellerPerformanceRepository extends JpaRepository<SellerPerformanceView, UUID> {
}
