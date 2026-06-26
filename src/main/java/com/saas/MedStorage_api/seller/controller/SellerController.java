package com.saas.MedStorage_api.seller.controller;

import com.saas.MedStorage_api.seller.dto.SellerPerformanceResponse;
import com.saas.MedStorage_api.seller.service.SellerPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Vendedores", description = "Performance de vendedores no mês corrente (pedidos RETIRADO)")
@RestController
@RequestMapping("/api/sellers")
public class SellerController {

    private final SellerPerformanceService performanceService;

    public SellerController(SellerPerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @Operation(summary = "Minha performance", description = "Retorna a performance do vendedor autenticado no mês corrente. Sem pedidos RETIRADO no mês retorna zeros.")
    @ApiResponse(responseCode = "200", description = "Performance do vendedor logado")
    @GetMapping("/performance")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<SellerPerformanceResponse> getMyPerformance(Authentication authentication) {
        return ResponseEntity.ok(performanceService.getMyPerformance(authentication));
    }

    @Operation(summary = "Performance de todos os vendedores", description = "Retorna a performance de todos os vendedores ativos no mês corrente. Requer ADMIN")
    @ApiResponse(responseCode = "200", description = "Lista de performance por vendedor")
    @GetMapping("/performance/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SellerPerformanceResponse>> getAllPerformance() {
        return ResponseEntity.ok(performanceService.getAllPerformance());
    }
}
