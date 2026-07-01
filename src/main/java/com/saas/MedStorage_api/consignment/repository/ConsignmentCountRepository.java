package com.saas.MedStorage_api.consignment.repository;

import com.saas.MedStorage_api.consignment.entity.ConsignmentCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsignmentCountRepository extends JpaRepository<ConsignmentCount, UUID> {

    List<ConsignmentCount> findByCustomer_IdOrderByDataContagemDesc(UUID customerId);

    List<ConsignmentCount> findAllByOrderByDataContagemDesc();
}
