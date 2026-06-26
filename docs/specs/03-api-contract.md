# 03. Contrato de API

Base path: `/api`. Autenticação: JWT Bearer (exceto `/api/auth/login`).

## Autenticação
```
POST   /api/auth/login        → { token, user }     (401 se credenciais inválidas, 404 se usuário não existe)
POST   /api/auth/logout       → invalida token
POST   /api/auth/refresh      → novo token
POST   /api/auth/register     → cria usuário (admin only)
GET    /api/auth/validate     → { valid, email, role }
```

## Pedidos (orders)
```
POST   /api/orders                 → criar pedido (vendedor, admin)            → 201       ✅ implementado
GET    /api/orders                 → listar com filtros + paginação            → 200       ✅ implementado
GET    /api/orders/{id}            → detalhe                                   → 200 / 404 ✅ implementado
PATCH  /api/orders/{id}/status     → mudar status (gerente, admin)             → 200 / 400 ✅ implementado
PUT    /api/orders/{id}            → editar pedido (somente status=PENDENTE)   → 200       🔜 Sprint 6+
DELETE /api/orders/{id}            → deletar (somente status=PENDENTE)         → 204       🔜 Sprint 6+
```

Filtros de `GET /api/orders`: `status`, `customerId`, `criadoPor`, `dataInicio`, `dataFim`, `valorMin`, `valorMax`, `page`, `size`, `sort`. Paginação: 20 itens/página por padrão.

## Clientes (customers)
```
POST   /api/customers                → criar                       → 201       ✅ implementado
GET    /api/customers                → listar paginado             → 200       ✅ implementado
GET    /api/customers/{id}           → detalhe                     → 200 / 404 ✅ implementado
PUT    /api/customers/{id}           → editar                      → 200       ✅ implementado
GET    /api/customers/{id}/orders    → histórico de pedidos        → 200       🔜 Sprint 6+
```

## Produtos / Estoque
```
GET    /api/products                  → listar ativos paginado                    → 200       ✅ implementado
GET    /api/products/{id}             → detalhe                                   → 200 / 404 ✅ implementado
GET    /api/inventory/status          → status OK/BAIXO/CRÍTICO de todos produtos → 200       ✅ implementado
GET    /api/inventory/{productId}     → status de um produto                      → 200 / 404 ✅ implementado
```

> Movimentos de estoque são criados internamente pelo `OrderService` ao marcar ATENDIDO (e futuramente por devoluções). Não há endpoint público de criação de movimentos no MVP.

## Devoluções
```
POST   /api/returns                   → registrar devolução (pedido deve estar RETIRADO) → 201       ✅ implementado
GET    /api/returns                   → listar paginado                                 → 200       ✅ implementado
GET    /api/returns/{id}              → detalhe                                         → 200 / 404 ✅ implementado
PATCH  /api/returns/{id}/process      → processar (incrementa estoque, tipo IN)         → 200       ✅ implementado
```

Fluxo de status: `PENDENTE → PROCESSADO`. Devolução só é permitida em pedido `RETIRADO`.
Número gerado automaticamente: `DEV-NNNNNN` (sequence no banco, gerado em Java como o `PED-NNNNNN` dos pedidos).

## Performance / Comissões
```
GET    /api/sellers/performance       → performance do vendedor logado no mês corrente     → 200  ✅ implementado
GET    /api/sellers/performance/all   → performance de todos os vendedores (admin)         → 200  ✅ implementado
GET    /api/commissions               → listar comissões paginado (filtro ?status=)        → 200  ✅ implementado
```

Performance baseia-se na view `vw_seller_performance_current_month` (pedidos `RETIRADO` no mês corrente). Sem pedidos no mês retorna zeros. `GET /api/commissions` é restrito a `ADMIN`; `GET /api/sellers/performance` aceita `VENDEDOR` ou `ADMIN`; `/all` aceita apenas `ADMIN`.

## Convenções de erro (`GlobalExceptionHandler` via `@RestControllerAdvice`)
```json
{ "error": "mensagem descritiva", "status": 400 }
```
- 400: validação de payload / regra de negócio (ex.: estoque insuficiente, desconto > 50%, transição de status inválida)
- 401: não autenticado / credenciais inválidas
- 403: autenticado sem permissão para a ação
- 404: recurso não encontrado

Implementado em `exception/GlobalExceptionHandler.java` com `ApiError(String error, int status)`.

## Documentação
Swagger UI disponível em `/swagger-ui.html` via `springdoc-openapi-starter-webmvc-ui:2.8.5`.
Controllers anotados com `@Tag`, `@Operation` e `@ApiResponse`. Autenticação JWT configurada via `OpenApiConfig.java`.
