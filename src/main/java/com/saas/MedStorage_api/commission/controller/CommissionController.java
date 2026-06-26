package com.saas.MedStorage_api.commission.controller;

import com.saas.MedStorage_api.commission.dto.CommissionResponse;
import com.saas.MedStorage_api.commission.enums.CommissionStatus;
import com.saas.MedStorage_api.commission.service.CommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Comissões", description = "Consulta de comissões de vendedores")
@RestController
@RequestMapping("/api/commissions")
public class CommissionController {

    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @Operation(summary = "Listar comissões", description = "Lista comissões paginadas. Filtro opcional por status (PENDENTE/PAGO). Requer ADMIN")
    @ApiResponse(responseCode = "200", description = "Lista de comissões")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CommissionResponse>> findAll(
            @RequestParam(required = false) CommissionStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commissionService.findAll(status, pageable));
    }
}
