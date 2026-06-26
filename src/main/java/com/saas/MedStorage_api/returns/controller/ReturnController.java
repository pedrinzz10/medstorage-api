package com.saas.MedStorage_api.returns.controller;

import com.saas.MedStorage_api.returns.dto.CreateReturnRequest;
import com.saas.MedStorage_api.returns.dto.ReturnResponse;
import com.saas.MedStorage_api.returns.service.ReturnService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Devoluções", description = "Registro e processamento de devoluções. Fluxo: criar (PENDENTE) → processar (PROCESSADO, reverte estoque)")
@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @Operation(summary = "Registrar devolução", description = "Cria uma devolução em status PENDENTE para um pedido RETIRADO. Requer papel VENDEDOR ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Devolução registrada"),
        @ApiResponse(responseCode = "400", description = "Pedido não está RETIRADO, produto não pertence ao pedido ou quantidade excede o pedido"),
        @ApiResponse(responseCode = "404", description = "Pedido ou produto não encontrado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<ReturnResponse> create(
            @Valid @RequestBody CreateReturnRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(returnService.create(request, authentication));
    }

    @Operation(summary = "Buscar devolução por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Devolução encontrada"),
        @ApiResponse(responseCode = "404", description = "Devolução não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReturnResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(returnService.findById(id));
    }

    @Operation(summary = "Listar devoluções", description = "Lista paginada de devoluções")
    @ApiResponse(responseCode = "200", description = "Lista de devoluções")
    @GetMapping
    public ResponseEntity<Page<ReturnResponse>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(returnService.findAll(pageable));
    }

    @Operation(summary = "Processar devolução", description = "Processa uma devolução PENDENTE: reverte estoque com movimento IN para cada item. Requer papel GERENTE_ESTOQUE ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Devolução processada, estoque revertido"),
        @ApiResponse(responseCode = "400", description = "Devolução não está PENDENTE"),
        @ApiResponse(responseCode = "404", description = "Devolução não encontrada")
    })
    @PatchMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('GERENTE_ESTOQUE', 'ADMIN')")
    public ResponseEntity<ReturnResponse> process(
            @PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(returnService.process(id, authentication));
    }
}
