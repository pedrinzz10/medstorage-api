package com.saas.MedStorage_api.order.controller;

import com.saas.MedStorage_api.order.dto.ChangeOrderStatusRequest;
import com.saas.MedStorage_api.order.dto.CreateOrderRequest;
import com.saas.MedStorage_api.order.dto.OrderResponse;
import com.saas.MedStorage_api.order.enums.OrderStatus;
import com.saas.MedStorage_api.order.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Pedidos", description = "Criação e gerenciamento de pedidos. Fluxo: CRIADO → CONFIRMADO → SEPARADO (reserva estoque) → PRONTO (e-mail) → FINALIZADO (baixa estoque + comissão)")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Criar pedido", description = "Cria um pedido em status CRIADO. Requer papel VENDEDOR ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pedido criado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou desconto acima de 50%"),
        @ApiResponse(responseCode = "404", description = "Cliente ou produto não encontrado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request, authentication));
    }

    @Operation(summary = "Buscar pedido por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @Operation(summary = "Listar pedidos", description = "Lista paginada com filtros opcionais: status, cliente, responsável, período, faixa de valor")
    @ApiResponse(responseCode = "200", description = "Lista de pedidos")
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> findAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID criadoPor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false) BigDecimal valorMin,
            @RequestParam(required = false) BigDecimal valorMax,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.findAll(
                status, customerId, criadoPor, dataInicio, dataFim, valorMin, valorMax, pageable));
    }

    @Operation(summary = "Editar pedido", description = "Permite alterar itens, cliente, desconto e notas. Somente pedidos CRIADO podem ser editados. Requer VENDEDOR ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido atualizado"),
        @ApiResponse(responseCode = "400", description = "Pedido não está em PENDENTE ou dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<OrderResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.update(id, request, authentication));
    }

    @Operation(summary = "Excluir pedido", description = "Remove um pedido. Somente pedidos CRIADO podem ser excluídos. Requer VENDEDOR ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pedido excluído"),
        @ApiResponse(responseCode = "400", description = "Pedido não está em PENDENTE"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public void delete(@PathVariable UUID id) {
        orderService.delete(id);
    }

    @Operation(summary = "Mudar status do pedido", description = "Transições: CRIADO→CONFIRMADO, CONFIRMADO→SEPARADO (reserva estoque), SEPARADO→PRONTO (e-mail ao cliente), PRONTO→FINALIZADO (baixa estoque + comissão), qualquer→CANCELADO. Requer GERENTE_ESTOQUE ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status atualizado"),
        @ApiResponse(responseCode = "400", description = "Transição de status inválida ou estoque insuficiente"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('GERENTE_ESTOQUE', 'ADMIN')")
    public ResponseEntity<OrderResponse> changeStatus(
            @PathVariable UUID id, @Valid @RequestBody ChangeOrderStatusRequest request, Authentication authentication) {
        return ResponseEntity.ok(orderService.changeStatus(id, request.newStatus(), authentication));
    }
}
