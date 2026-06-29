package com.saas.MedStorage_api.commission.repository;

import com.saas.MedStorage_api.commission.entity.Commission;
import com.saas.MedStorage_api.commission.enums.CommissionStatus;
import com.saas.MedStorage_api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CommissionRepository extends JpaRepository<Commission, UUID> {

    Page<Commission> findByStatus(CommissionStatus status, Pageable pageable);

    Optional<Commission> findByVendedorAndPeriodoInicioAndPeriodoFim(
            User vendedor, LocalDate periodoInicio, LocalDate periodoFim);
}
