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
POST   /api/orders                 → criar pedido (vendedor, admin)            → 201
GET    /api/orders                 → listar com filtros + paginação           → 200
GET    /api/orders/{id}             → detalhe                                  → 200 / 404
PUT    /api/orders/{id}             → editar (somente status=PENDENTE)         → 200
DELETE /api/orders/{id}             → deletar (somente status=PENDENTE)        → 204
PATCH  /api/orders/{id}/status      → mudar status (gerente, admin)            → 200 / 400 (estoque insuf.)
```

Filtros de `GET /api/orders`: `status`, `customerId`, `criadoPor`, `dataInicio`, `dataFim`, `valorMin`, `valorMax`, `page`, `size`, `sort`. Paginação: 20 itens/página por padrão. Filtros devem ser refletíveis na URL (frontend).

## Clientes (customers)
```
POST   /api/customers                → criar                       → 201
GET    /api/customers                → listar                      → 200
GET    /api/customers/{id}            → detalhe + resumo de compras → 200 / 404
PUT    /api/customers/{id}            → editar                      → 200
GET    /api/customers/{id}/orders     → histórico de pedidos        → 200
```

## Produtos / Estoque
```
GET    /api/products                  → listar paginado             → 200
GET    /api/products/{id}             → detalhe                     → 200 / 404
GET    /api/inventory                 → listar com estoque          → 200
GET    /api/inventory/status          → status OK/BAIXO/CRÍTICO     → 200
GET    /api/inventory/{productId}     → detalhe                     → 200 / 404
POST   /api/inventory/movements       → histórico de movimentos     → 200
```

## Devoluções
```
POST   /api/returns                   → registrar devolução (pedido deve estar RETIRADO) → 201
GET    /api/returns                   → listar                                          → 200
PATCH  /api/returns/{id}/process       → processar (incrementa estoque)                 → 200
```

## Performance / Comissões
```
GET    /api/sellers/performance       → performance do vendedor logado → 200
GET    /api/sellers/performance/all   → performance de todos (admin)   → 200
GET    /api/commissions               → comissões pendentes             → 200
```

## Convenções de erro (padronizar via `@ControllerAdvice`)
```json
{ "error": "mensagem", "status": 400 }
```
- 400: validação de payload / regra de negócio (ex.: estoque insuficiente, desconto > 50%)
- 401: não autenticado / credenciais inválidas
- 403: autenticado sem permissão para a ação
- 404: recurso não encontrado

## Documentação
Expor Swagger UI via `springdoc-openapi-starter-webmvc-ui` em `/swagger-ui.html`, com schemas gerados a partir dos DTOs.
