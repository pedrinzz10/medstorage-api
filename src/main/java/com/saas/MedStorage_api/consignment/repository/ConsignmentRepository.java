package com.saas.MedStorage_api.consignment.repository;

import com.saas.MedStorage_api.consignment.entity.Consignment;
import com.saas.MedStorage_api.consignment.enums.ConsignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsignmentRepository extends JpaRepository<Consignment, UUID> {

    Page<Consignment> findByCustomer_IdAndStatus(UUID customerId, ConsignmentStatus status, Pageable pageable);

    Page<Consignment> findByCustomer_Id(UUID customerId, Pageable pageable);

    Page<Consignment> findByStatus(ConsignmentStatus status, Pageable pageable);

    List<Consignment> findByCustomer_IdAndStatus(UUID customerId, ConsignmentStatus status);
}
