package com.saas.MedStorage_api.inventorymovement.controller;

import com.saas.MedStorage_api.inventorymovement.dto.InventoryMovementResponse;
import com.saas.MedStorage_api.inventorymovement.dto.StockAdjustmentRequest;
import com.saas.MedStorage_api.inventorymovement.service.InventoryMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import java.util.UUID;

@Tag(name = "Movimentos de Estoque", description = "Histórico de movimentações e entrada manual de estoque")
@RestController
@RequestMapping("/api/inventory/movements")
public class InventoryMovementController {

    private final InventoryMovementService movementService;

    public InventoryMovementController(InventoryMovementService movementService) {
        this.movementService = movementService;
    }

    @Operation(summary = "Listar movimentos de estoque", description = "Lista paginada de movimentos IN/OUT. Filtro opcional por produto (?productId=)")
    @ApiResponse(responseCode = "200", description = "Lista de movimentos")
    @GetMapping
    public ResponseEntity<Page<InventoryMovementResponse>> findAll(
            @RequestParam(required = false) UUID productId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(movementService.findAll(productId, pageable));
    }

    @Operation(summary = "Repor estoque manualmente", description = "Registra uma entrada (IN) manual de estoque para um produto. Requer GERENTE_ESTOQUE ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Estoque reposto com sucesso"),
        @ApiResponse(responseCode = "400", description = "Quantidade inválida ou campos obrigatórios ausentes"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado no estoque")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE_ESTOQUE', 'ADMIN')")
    public ResponseEntity<InventoryMovementResponse> adjust(
            @Valid @RequestBody StockAdjustmentRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(movementService.adjust(request, authentication));
    }
}
