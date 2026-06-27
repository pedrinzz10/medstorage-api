package com.saas.MedStorage_api.customer.repository;

import com.saas.MedStorage_api.customer.entity.CustomerSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerSummaryRepository extends JpaRepository<CustomerSummaryView, UUID> {
}
