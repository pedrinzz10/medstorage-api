package com.saas.MedStorage_api.inventory.controller;

import com.saas.MedStorage_api.inventory.dto.InventoryStatusResponse;
import com.saas.MedStorage_api.inventory.service.InventoryService;
import com.saas.MedStorage_api.inventory.service.StockAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Estoque", description = "Consulta de status de estoque com classificação de criticidade (OK / BAIXO / CRITICO)")
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final StockAlertService stockAlertService;

    public InventoryController(InventoryService inventoryService, StockAlertService stockAlertService) {
        this.inventoryService = inventoryService;
        this.stockAlertService = stockAlertService;
    }

    @Operation(summary = "Status geral do estoque", description = "Lista todos os produtos ativos com quantidade atual e classificação de criticidade, ordenados por severidade")
    @ApiResponse(responseCode = "200", description = "Status de estoque de todos os produtos")
    @GetMapping("/status")
    public ResponseEntity<List<InventoryStatusResponse>> getStatus() {
        return ResponseEntity.ok(inventoryService.getStatus());
    }

    @Operation(summary = "Disparar alerta de estoque baixo manualmente", description = "Envia imediatamente o email de estoque crítico para ADMIN/GERENTE_ESTOQUE, sem esperar o agendamento diário. Requer ADMIN")
    @ApiResponse(responseCode = "200", description = "Alerta processado (pode não ter enviado nenhum email se não houver produtos críticos)")
    @PostMapping("/low-stock-alert/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> triggerLowStockAlert() {
        int criticos = stockAlertService.checkAndNotify();
        return ResponseEntity.ok(Map.of("produtosCriticos", criticos));
    }

    @Operation(summary = "Status de estoque de um produto")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status encontrado"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado no estoque")
    })
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryStatusResponse> findByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.findByProductId(productId));
    }
}
