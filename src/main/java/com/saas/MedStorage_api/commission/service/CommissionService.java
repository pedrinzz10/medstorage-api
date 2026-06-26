package com.saas.MedStorage_api.commission.service;

import com.saas.MedStorage_api.commission.dto.CommissionResponse;
import com.saas.MedStorage_api.commission.enums.CommissionStatus;
import com.saas.MedStorage_api.commission.repository.CommissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CommissionService {

    private final CommissionRepository commissionRepository;

    public CommissionService(CommissionRepository commissionRepository) {
        this.commissionRepository = commissionRepository;
    }

    public Page<CommissionResponse> findAll(CommissionStatus status, Pageable pageable) {
        log.debug("Listando comissoes: status={}", status);
        if (status != null) {
            return commissionRepository.findByStatus(status, pageable).map(CommissionResponse::from);
        }
        return commissionRepository.findAll(pageable).map(CommissionResponse::from);
    }
}
