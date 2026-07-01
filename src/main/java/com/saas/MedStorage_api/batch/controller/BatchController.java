package com.saas.MedStorage_api.batch.controller;

import com.saas.MedStorage_api.batch.dto.BatchOrderTraceResponse;
import com.saas.MedStorage_api.batch.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Rastreabilidade de Lotes", description = "Consulta reversa: quais pedidos consumiram um determinado lote")
@RestController
@RequestMapping("/api/inventory/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @Operation(summary = "Rastrear pedidos que consumiram um lote", description = "Lista os pedidos (separados/finalizados) que consumiram unidades deste lote, para suporte a recall. Requer GERENTE_ESTOQUE ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de pedidos que consumiram o lote"),
        @ApiResponse(responseCode = "404", description = "Lote não encontrado")
    })
    @GetMapping("/{batchId}/orders")
    @PreAuthorize("hasAnyRole('GERENTE_ESTOQUE', 'ADMIN')")
    public ResponseEntity<List<BatchOrderTraceResponse>> ordersForBatch(@PathVariable UUID batchId) {
        return ResponseEntity.ok(batchService.findOrdersForBatch(batchId));
    }
}
