package com.saas.MedStorage_api.consignment.repository;

import com.saas.MedStorage_api.consignment.entity.ConsignmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsignmentItemRepository extends JpaRepository<ConsignmentItem, UUID> {
}
