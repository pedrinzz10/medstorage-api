package com.saas.MedStorage_api.seller.service;

import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.seller.dto.SellerPerformanceResponse;
import com.saas.MedStorage_api.seller.repository.SellerPerformanceRepository;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SellerPerformanceService {

    private final SellerPerformanceRepository performanceRepository;
    private final UserRepository userRepository;

    public SellerPerformanceService(
            SellerPerformanceRepository performanceRepository,
            UserRepository userRepository) {
        this.performanceRepository = performanceRepository;
        this.userRepository = userRepository;
    }

    public SellerPerformanceResponse getMyPerformance(Authentication authentication) {
        User user = currentUser(authentication);
        log.debug("Consultando performance do mes corrente: user={}", user.getEmail());
        return performanceRepository.findById(user.getId())
                .map(SellerPerformanceResponse::from)
                .orElse(SellerPerformanceResponse.empty(user));
    }

    public List<SellerPerformanceResponse> getAllPerformance() {
        log.debug("Consultando performance de todos os vendedores no mes corrente");
        return performanceRepository.findAll()
                .stream()
                .map(SellerPerformanceResponse::from)
                .toList();
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
