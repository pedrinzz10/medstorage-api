package com.saas.MedStorage_api.commission.repository;

import com.saas.MedStorage_api.commission.entity.Commission;
import com.saas.MedStorage_api.commission.enums.CommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommissionRepository extends JpaRepository<Commission, UUID> {

    Page<Commission> findByStatus(CommissionStatus status, Pageable pageable);
}
