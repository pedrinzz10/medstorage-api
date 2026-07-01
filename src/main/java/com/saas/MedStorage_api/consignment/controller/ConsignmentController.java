package com.saas.MedStorage_api.consignment.controller;

import com.saas.MedStorage_api.consignment.dto.ConsignmentItemResponse;
import com.saas.MedStorage_api.consignment.dto.ConsignmentResponse;
import com.saas.MedStorage_api.consignment.dto.ConsignmentUsageResponse;
import com.saas.MedStorage_api.consignment.dto.CreateConsignmentRequest;
import com.saas.MedStorage_api.consignment.dto.RegisterReturnRequest;
import com.saas.MedStorage_api.consignment.dto.RegisterUsageRequest;
import com.saas.MedStorage_api.consignment.enums.ConsignmentStatus;
import com.saas.MedStorage_api.consignment.service.ConsignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Consignação", description = "Material enviado a clientes em consignação: remessa, uso/faturamento e devolução")
@RestController
@RequestMapping("/api/consignments")
public class ConsignmentController {

    private final ConsignmentService consignmentService;

    public ConsignmentController(ConsignmentService consignmentService) {
        this.consignmentService = consignmentService;
    }

    @Operation(summary = "Enviar material em consignação", description = "Cria uma remessa de consignação, dá baixa no estoque (lote único por item) e registra a movimentação. Requer VENDEDOR ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Consignação criada"),
        @ApiResponse(responseCode = "400", description = "Estoque insuficiente ou dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Cliente ou produto não encontrado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<ConsignmentResponse> create(
            @Valid @RequestBody CreateConsignmentRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consignmentService.create(request, authentication));
    }

    @Operation(summary = "Buscar consignação por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Consignação encontrada"),
        @ApiResponse(responseCode = "404", description = "Consignação não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ConsignmentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(consignmentService.findById(id));
    }

    @Operation(summary = "Listar consignações", description = "Lista paginada com filtros opcionais de cliente e status")
    @ApiResponse(responseCode = "200", description = "Lista de consignações")
    @GetMapping
    public ResponseEntity<Page<ConsignmentResponse>> findAll(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) ConsignmentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(consignmentService.findAll(customerId, status, pageable));
    }

    @Operation(summary = "Registrar uso/faturamento", description = "Hospital reportou uso do material — registra a quantidade faturada. Requer VENDEDOR ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Uso registrado"),
        @ApiResponse(responseCode = "400", description = "Quantidade excede o saldo disponível"),
        @ApiResponse(responseCode = "404", description = "Consignação ou item não encontrado")
    })
    @PostMapping("/{id}/items/{itemId}/usage")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<ConsignmentUsageResponse> registerUsage(
            @PathVariable UUID id, @PathVariable UUID itemId,
            @Valid @RequestBody RegisterUsageRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consignmentService.registerUsage(id, itemId, request, authentication));
    }

    @Operation(summary = "Registrar devolução", description = "Material consignado não usado volta ao estoque. Requer VENDEDOR ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Devolução registrada"),
        @ApiResponse(responseCode = "400", description = "Quantidade excede o saldo disponível"),
        @ApiResponse(responseCode = "404", description = "Consignação ou item não encontrado")
    })
    @PostMapping("/{id}/items/{itemId}/return")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<ConsignmentItemResponse> registerReturn(
            @PathVariable UUID id, @PathVariable UUID itemId,
            @Valid @RequestBody RegisterReturnRequest request, Authentication authentication) {
        return ResponseEntity.ok(consignmentService.registerReturn(id, itemId, request, authentication));
    }

    @Operation(summary = "Encerrar consignação manualmente", description = "Baixa administrativa (ex.: perda confirmada). Requer ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Consignação encerrada"),
        @ApiResponse(responseCode = "404", description = "Consignação não encontrada")
    })
    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConsignmentResponse> close(@PathVariable UUID id) {
        return ResponseEntity.ok(consignmentService.close(id));
    }
}
