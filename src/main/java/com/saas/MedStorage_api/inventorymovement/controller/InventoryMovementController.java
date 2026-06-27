package com.saas.MedStorage_api.inventorymovement.controller;

import com.saas.MedStorage_api.inventorymovement.dto.InventoryMovementResponse;
import com.saas.MedStorage_api.inventorymovement.service.InventoryMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Movimentos de Estoque", description = "Histórico de entradas e saídas de estoque geradas por pedidos e devoluções")
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
}
