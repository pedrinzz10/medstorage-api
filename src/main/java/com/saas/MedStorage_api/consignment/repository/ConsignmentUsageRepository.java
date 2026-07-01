package com.saas.MedStorage_api.consignment.repository;

import com.saas.MedStorage_api.consignment.entity.ConsignmentUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsignmentUsageRepository extends JpaRepository<ConsignmentUsage, UUID> {
}
