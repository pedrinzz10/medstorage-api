package com.saas.MedStorage_api.consignment.controller;

import com.saas.MedStorage_api.consignment.dto.ConsignmentCountResponse;
import com.saas.MedStorage_api.consignment.dto.RegisterCountRequest;
import com.saas.MedStorage_api.consignment.service.ConsignmentCountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Contagem de Consignação", description = "Contagem física mensal do material consignado no cliente, com divergência por item")
@RestController
@RequestMapping("/api/consignments/counts")
public class ConsignmentCountController {

    private final ConsignmentCountService consignmentCountService;

    public ConsignmentCountController(ConsignmentCountService consignmentCountService) {
        this.consignmentCountService = consignmentCountService;
    }

    @Operation(summary = "Registrar contagem de material consignado", description = "Confere quantidade/lote/validade de cada item consignado ativo do cliente e calcula a divergência (positiva = falta material, provavelmente usado e não reportado). Se vinculada a uma visita, marca a visita como REALIZADA. Requer GERENTE_ESTOQUE ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Contagem registrada"),
        @ApiResponse(responseCode = "400", description = "Item não pertence ao cliente informado"),
        @ApiResponse(responseCode = "404", description = "Cliente, visita ou item não encontrado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE_ESTOQUE', 'ADMIN')")
    public ResponseEntity<ConsignmentCountResponse> registerCount(
            @Valid @RequestBody RegisterCountRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consignmentCountService.registerCount(request, authentication));
    }

    @Operation(summary = "Histórico de contagens", description = "Lista todas as contagens registradas, mais recente primeiro. Filtro opcional por cliente")
    @ApiResponse(responseCode = "200", description = "Lista de contagens")
    @GetMapping
    public ResponseEntity<List<ConsignmentCountResponse>> findByCustomer(
            @RequestParam(required = false) UUID customerId) {
        return ResponseEntity.ok(consignmentCountService.findByCustomer(customerId));
    }
}
