package com.saas.MedStorage_api.consignment.controller;

import com.saas.MedStorage_api.consignment.dto.CreateVisitRequest;
import com.saas.MedStorage_api.consignment.dto.VisitResponse;
import com.saas.MedStorage_api.consignment.service.ConsignmentVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Calendário de Visitas", description = "Agendamento de visitas para conferência mensal de material consignado")
@RestController
@RequestMapping("/api/consignment-visits")
public class ConsignmentVisitController {

    private final ConsignmentVisitService visitService;

    public ConsignmentVisitController(ConsignmentVisitService visitService) {
        this.visitService = visitService;
    }

    @Operation(summary = "Agendar visita", description = "Requer GERENTE_ESTOQUE ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Visita agendada"),
        @ApiResponse(responseCode = "404", description = "Cliente ou funcionário não encontrado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE_ESTOQUE', 'ADMIN')")
    public ResponseEntity<VisitResponse> create(
            @Valid @RequestBody CreateVisitRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitService.create(request, authentication));
    }

    @Operation(summary = "Listar visitas por período", description = "Usado pelo calendário para carregar o mês visível")
    @ApiResponse(responseCode = "200", description = "Lista de visitas no período")
    @GetMapping
    public ResponseEntity<List<VisitResponse>> findByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(visitService.findByRange(from, to));
    }

    @Operation(summary = "Cancelar visita", description = "Requer GERENTE_ESTOQUE ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Visita cancelada"),
        @ApiResponse(responseCode = "400", description = "Visita já realizada"),
        @ApiResponse(responseCode = "404", description = "Visita não encontrada")
    })
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('GERENTE_ESTOQUE', 'ADMIN')")
    public ResponseEntity<VisitResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(visitService.cancel(id));
    }
}
