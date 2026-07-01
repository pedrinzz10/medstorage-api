package com.saas.MedStorage_api.consignment.repository;

import com.saas.MedStorage_api.consignment.entity.ConsignmentVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ConsignmentVisitRepository extends JpaRepository<ConsignmentVisit, UUID> {

    List<ConsignmentVisit> findByDataAgendadaBetween(LocalDate from, LocalDate to);
}
