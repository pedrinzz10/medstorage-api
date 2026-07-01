package com.saas.MedStorage_api.product.controller;

import com.saas.MedStorage_api.batch.dto.BatchResponse;
import com.saas.MedStorage_api.batch.service.BatchService;
import com.saas.MedStorage_api.product.dto.ProductAbcResponse;
import com.saas.MedStorage_api.product.dto.ProductRequest;
import com.saas.MedStorage_api.product.dto.ProductResponse;
import com.saas.MedStorage_api.product.service.AbcAnalysisService;
import com.saas.MedStorage_api.product.service.ProductService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Produtos", description = "Consulta do catálogo de produtos (materiais médicos)")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final AbcAnalysisService abcAnalysisService;
    private final BatchService batchService;

    public ProductController(ProductService productService, AbcAnalysisService abcAnalysisService, BatchService batchService) {
        this.productService = productService;
        this.abcAnalysisService = abcAnalysisService;
        this.batchService = batchService;
    }

    @Operation(summary = "Listar produtos ativos", description = "Retorna lista paginada de produtos com ativo=true")
    @ApiResponse(responseCode = "200", description = "Lista de produtos")
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productService.findAll(pageable));
    }

    @Operation(summary = "Buscar produto por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto encontrado"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @Operation(summary = "Curva ABC de produtos", description = "Classifica produtos por relevância de valor vendido (pedidos FINALIZADO): classe A cobre até 80% do valor acumulado, B até 95%, C o restante. Requer GERENTE_ESTOQUE ou ADMIN")
    @ApiResponse(responseCode = "200", description = "Lista de produtos classificados, ordenada por valor vendido decrescente")
    @GetMapping("/abc-analysis")
    @PreAuthorize("hasAnyRole('GERENTE_ESTOQUE', 'ADMIN')")
    public ResponseEntity<List<ProductAbcResponse>> abcAnalysis() {
        return ResponseEntity.ok(abcAnalysisService.classify());
    }

    @Operation(summary = "Listar lotes de um produto", description = "Lista os lotes com quantidade e validade, ordenados por validade crescente (mais próximos do vencimento primeiro). Requer GERENTE_ESTOQUE ou ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de lotes do produto"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    @GetMapping("/{id}/batches")
    @PreAuthorize("hasAnyRole('GERENTE_ESTOQUE', 'ADMIN')")
    public ResponseEntity<List<BatchResponse>> batches(@PathVariable UUID id) {
        return ResponseEntity.ok(batchService.findByProduct(id));
    }

    @Operation(summary = "Criar produto", description = "Cria um novo produto e entrada de estoque zerada. Requer ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Produto criado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou SKU duplicado")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @Operation(summary = "Atualizar produto", description = "Atualiza dados do produto. Requer ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto atualizado"),
        @ApiResponse(responseCode = "400", description = "SKU duplicado ou dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @Operation(summary = "Desativar produto", description = "Desativa o produto (soft delete — ativo=false). Requer ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Produto desativado"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable UUID id) {
        productService.deactivate(id);
    }
}
