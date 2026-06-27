# 05. Roadmap de Implementação (Backend)

Baseado em `docs/inicial_docs/BACKEND_GUIA_COMPLETO.md`. Critério de saída de cada sprint: checklist abaixo + cobertura de testes ≥ 80%.

## Status atual

| Sprint | Status | PR | Cobertura |
|---|---|---|---|
| 1 — Setup + Autenticação | ✅ Concluída | #1, #2, #3 | 90% |
| 2 — Produtos + Estoque | ✅ Concluída | #4 | 93% |
| 3 — CRUD Clientes | ✅ Concluída | #5 | 94% |
| 4 — Pedidos: criação + ATENDIDO | ✅ Concluída | #6 | 87% |
| 5 — RETIRADO + filtros avançados | ✅ Concluída | #8 | 86% |
| Email Gmail SMTP (complemento Sprint 4) | ✅ Concluída | #10 | — |
| Refactor — Estrutura modular (sub-pacotes) | ✅ Concluída | #11 | — |
| Portfolio — README, Docker, Swagger, Logging | ✅ Concluída | #12 | — |
| 6 — Devoluções | ✅ Concluída | #18 | — |
| 7 — Performance + Comissões | ✅ Concluída | #24 | — |
| 8 — Hardening e fechamento | ✅ Concluída | #26 | — |
| 9 — Edição/exclusão de pedidos + histórico de cliente | ✅ Concluída | #29 | — |
| 10 — Customer summary, inventory movements e rejeição de devoluções | ✅ Concluída | #31 | — |

## Sprint 1 — Setup + Autenticação
- `docker-compose.yml` com PostgreSQL local para desenvolvimento (`Dockerfile` e serviço `app` adicionados posteriormente — PR #12)
- Flyway configurado (`flyway-core`, `flyway-database-postgresql`) + primeira migration (`V1__create_users_table.sql`)
- Projeto Spring Boot compilando (`./gradlew build`)
- Entidade `User`, `UserRepository`
- `JwtProvider` (gera/valida token), `AuthService`, `AuthController`
- `SecurityConfig` (CORS, regras de acesso por papel)
- Testes: unitários de `AuthService`/`JwtProvider`, integração de `AuthController`
- Skill instalada: `spring-boot-security-jwt`

## Sprint 2 — Produtos + Estoque
- `Product`, `Inventory`, repositories
- `GET /api/products` paginado, `GET /api/inventory/status`
- Testes unitários de `ProductService`, integração de `ProductController`

## Sprint 3 — CRUD Clientes
- `Customer`, `CustomerRepository`
- `POST/GET/PUT /api/customers`, `GET /api/customers/{id}/orders`
- Validação de campos obrigatórios (nome, email)

## Sprint 4 — Pedidos: criação + marcar ATENDIDO
- `Order`, `OrderItem`, criação de pedido com itens
- `PATCH /api/orders/{id}/status` → ATENDIDO: decremento atômico de estoque + `inventory_movement` + email
- Testes de transação atômica (rollback em erro / estoque insuficiente)

## Sprint 5 — Marcar RETIRADO + listagem/filtros avançados
- `PATCH .../status` → RETIRADO
- Filtros de `GET /api/orders` (status, cliente, vendedor, data, valor) + paginação/ordenação

## Sprint 6 — Devoluções
- `Return`, `ReturnItem` — pacote `returns/` com sub-pacotes `entity/`, `dto/`, `repository/`, `service/`, `controller/`, `enums/`
- Migrations V11 (tabela `returns` + sequence `return_numero_seq` + trigger) e V12 (tabela `return_items`)
- `POST /api/returns` → cria devolução `PENDENTE` para pedido `RETIRADO`; valida que os produtos pertencem ao pedido e que as quantidades não excedem o pedido
- `GET /api/returns` / `GET /api/returns/{id}` → listagem paginada e detalhe
- `PATCH /api/returns/{id}/process` → status `PROCESSADO`, incrementa estoque com `InventoryMovement` tipo `IN`, motivo `"Devolução DEV-XXXXXX"`

## Sprint 7 — Performance de Vendedores + Comissões
- Migration V13: tabela `commissions` com `valor_comissao` como coluna gerada (`valor_vendido * taxa / 100`)
- Migration V14: view `vw_seller_performance_current_month` (pedidos `RETIRADO` no mês corrente, agrupados por vendedor)
- Módulo `seller/`: `SellerPerformanceView` (entidade `@Immutable` mapeada à view), service e controller
- Módulo `commission/`: entity, DTO, repository, service e controller
- `GET /api/sellers/performance` → performance do vendedor logado (zeros se sem pedidos no mês)
- `GET /api/sellers/performance/all` → todos os vendedores com pedidos no mês (ADMIN)
- `GET /api/commissions?status=` → listagem paginada de comissões (ADMIN)

## Sprint 8 — Hardening e fechamento
- ~~Swagger/OpenAPI completo~~ — **concluído antecipadamente** (PR #12: `@Tag`, `@Operation`, `@ApiResponse` em todos os controllers)
- **Rate limiting no login**: `LoginRateLimiter` (sliding window, 5 tentativas/min por IP, configurável via `security.login.rate-limit.*`). Retorna `429 Too Many Requests`. `@EnableScheduling` + `@Scheduled` para limpeza de janelas expiradas a cada 5 min
- **59 testes unitários passando** (6 novos: `LoginRateLimiterTest`)
- `TooManyRequestsException` + handler 429 no `GlobalExceptionHandler`
- `X-Forwarded-For` suportado para apps atrás de proxy/load balancer
- Cobertura ≥ 80% mantida (JaCoCo enforced no CI)

## Sprint 9 — Edição/exclusão de pedidos + histórico de cliente
- Migration V15: view `vw_customer_summary` (total_pedidos, valor_total_gasto, ultima_compra por cliente)
- `PUT /api/orders/{id}` → editar pedido `PENDENTE`: substitui itens, recalcula valor, valida desconto. Requer `VENDEDOR` ou `ADMIN`
- `DELETE /api/orders/{id}` → excluir pedido `PENDENTE` (204). Requer `VENDEDOR` ou `ADMIN`
- `GET /api/customers/{id}/orders` → histórico paginado de pedidos do cliente (404 se cliente inexistente)
- `OrderRepository.findByCustomer_Id` — derivado do Spring Data JPA
- 5 novos testes unitários (`OrderServiceTest`) + 7 novos testes de integração

## Decisões técnicas (confirmadas)
- **Migrations:** Flyway (`flyway-core` + `flyway-database-postgresql`), scripts versionados em `src/main/resources/db/migration`
- **Banco de dev:** PostgreSQL via Docker local (`docker-compose.yml` na raiz do projeto)
- **Email:** `spring-boot-starter-mail` configurado com Gmail SMTP (PR #10). Envio ocorre em `afterCommit()` para não acoplar ao rollback da transação. Credenciais via `.env` (`SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`).

## Definition of Done (por sprint)
- [ ] `./gradlew build` — BUILD SUCCESSFUL
- [ ] `./gradlew test` — 100% dos testes passando
- [ ] Cobertura ≥ 80% (Jacoco)
- [ ] Endpoints validados manualmente (curl/Postman) conforme exemplos em `03-api-contract.md`
- [ ] Sem warnings novos no build
