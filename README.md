# MedStorage API

[![CI](https://github.com/pedrinzz10/medstorage-api/actions/workflows/ci.yml/badge.svg)](https://github.com/pedrinzz10/medstorage-api/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue?logo=openjdk)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)
![JaCoCo 80%+](https://img.shields.io/badge/Coverage-80%25%2B-brightgreen)

API REST para gestão completa de pedidos, estoque, comissões e devoluções de uma distribuidora de materiais médicos. Construída com Java 21 e Spring Boot 4.1, autenticação JWT via cookie HttpOnly, versionamento de banco com Flyway e cobertura mínima de 80% enforced no CI.

---

## Sumário

- [Stack](#stack)
- [Arquitetura](#arquitetura)
- [Como executar](#como-executar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Autenticação](#autenticação)
- [Endpoints](#endpoints)
- [Fluxo de negócio](#fluxo-de-negócio)
- [Notificações por e-mail](#notificações-por-e-mail)
- [Banco de dados](#banco-de-dados)
- [Testes e cobertura](#testes-e-cobertura)
- [Decisões técnicas](#decisões-técnicas)
- [Documentação detalhada](#documentação-detalhada)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 · Spring Security 6 · Spring Data JPA |
| Banco de dados | PostgreSQL 16 |
| Autenticação | JWT stateless — JJWT 0.12 · cookie HttpOnly · SameSite Strict |
| Migrações | Flyway 11 (16 versões) |
| Validação | Jakarta Bean Validation 3.1 |
| Documentação | SpringDoc OpenAPI 2.8.5 (Swagger UI) |
| Notificação | Spring Mail via SMTP (Gmail) |
| Build | Gradle 8 |
| Testes | JUnit 5 · Spring MockMvc · JaCoCo 0.8.12 (mín. 80% enforced) |
| Container | Docker · Docker Compose |
| CI/CD | GitHub Actions |

---

## Arquitetura

Organização **package-by-feature**: cada módulo de negócio agrupa seus próprios `entity`, `dto`, `repository`, `service` e `controller`, tornando cada feature autocontida.

```
src/main/java/com/saas/MedStorage_api/
│
├── auth/               Login, registro, refresh, logout e rate limiting
├── user/               Gerenciamento de usuários (ADMIN)
├── customer/           Cadastro e histórico consolidado de clientes
├── product/            Catálogo com soft delete
├── inventory/          Saldo e status de criticidade de estoque
├── inventorymovement/  Histórico de movimentações (IN / OUT)
├── order/              Pedidos — ciclo de vida PENDENTE → ATENDIDO → RETIRADO
├── returns/            Devoluções — PENDENTE → PROCESSADO / REJEITADO
├── commission/         Comissões de vendedores por período
├── seller/             Performance de vendas por período (view)
│
├── security/           JwtProvider, JwtAuthenticationFilter, SecurityConfig
├── exception/          GlobalExceptionHandler + ApiError padronizado
└── config/             OpenAPI / Swagger
```

### Perfis de acesso (roles)

| Role | Valor no banco | Acesso |
|---|---|---|
| `ADMIN` | `admin` | Acesso total: usuários, produtos, comissões, relatórios |
| `GERENTE_ESTOQUE` | `gerente_estoque` | Aprova pedidos, processa devoluções, consulta estoque |
| `VENDEDOR` | `vendedor` | Cria pedidos, consulta própria performance |

> Regras completas por role: [`docs/specs/04-business-rules.md`](docs/specs/04-business-rules.md)

---

## Como executar

### Pré-requisitos

- Docker e Docker Compose
- Para desenvolvimento local sem Docker: JDK 21

### Opção 1 — Docker Compose (recomendado)

Sobe banco e aplicação em um único comando:

```bash
git clone https://github.com/pedrinzz10/medstorage-api.git
cd medstorage-api

cp .env.example .env
# Edite .env — pelo menos JWT_SECRET é obrigatório

docker compose up --build
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Opção 2 — Desenvolvimento local

```bash
# 1. Sobe apenas o banco
docker compose up postgres -d

# 2. Crie o .env e defina as variáveis (ver seção abaixo)
cp .env.example .env

# 3. Executa a aplicação
./gradlew bootRun
```

O Flyway aplica as 16 migrações automaticamente ao iniciar. Os dados de seed (usuários e produtos de exemplo) são inseridos nas migrações `V2` e `V6`.

> Guia completo de configuração e SMTP: [`docs/configuracao.md`](docs/configuracao.md)

---

## Variáveis de ambiente

| Variável | Obrigatória | Default | Descrição |
|---|---|---|---|
| `JWT_SECRET` | **Sim** | — | Chave HMAC-SHA para assinar tokens (mín. 32 chars) |
| `DB_NAME` | Não | `medstorage` | Nome do banco PostgreSQL |
| `DB_USER` | Não | `medstorage` | Usuário do banco |
| `DB_PASSWORD` | Não | `medstorage` | Senha do banco |
| `DB_PORT` | Não | `5432` | Porta do banco |
| `JWT_EXPIRATION_MS` | Não | `86400000` | Validade do token em ms (default: 24 h) |
| `MAIL_USERNAME` | Não | — | E-mail SMTP para notificações |
| `MAIL_PASSWORD` | Não | — | Senha de app do Gmail |
| `MAIL_FROM_NAME` | Não | `MedStorage` | Nome exibido no campo "De:" |
| `CORS_ALLOWED_ORIGINS` | Não | `http://localhost:3000` | Origens CORS aceitas, separadas por vírgula |
| `TRUSTED_PROXIES` | Não | `""` (vazio) | IPs de proxy cujo `X-Forwarded-For` é confiado |
| `COOKIE_SECURE` | Não | `false` | `true` em produção (HTTPS) — ativado automaticamente no perfil `prod` |

> Em produção `JWT_SECRET` é injetado como secret do GitHub Actions, nunca em arquivo commitado.

---

## Autenticação

A API usa **JWT stateless**. O token é entregue em um **cookie HttpOnly**, o que impede acesso pelo JavaScript do navegador (proteção contra XSS). Clientes de API como Swagger ou ferramentas de linha de comando podem usar o header `Authorization: Bearer <token>` — o filtro aceita as duas formas.

### Login

```bash
POST /api/auth/login
Content-Type: application/json

{ "email": "admin@distribuidor.com", "password": "Admin123!" }
```

Resposta:
```
HTTP/1.1 200 OK
Set-Cookie: jwt=<token>; HttpOnly; SameSite=Strict; Path=/
Content-Type: application/json

{ "id": "...", "email": "admin@distribuidor.com", "nome": "Administrador", "role": "admin" }
```

O corpo da resposta retorna apenas os dados do usuário — o token não aparece no JSON, somente no cookie.

### Fazendo requisições autenticadas

**Via cookie** (navegador, frontend): o cookie é enviado automaticamente em cada requisição.

**Via header** (Swagger, ferramentas de API):
```
Authorization: Bearer <token>
```

### Rate limiting

O endpoint `/api/auth/login` aceita no máximo **5 tentativas por minuto por IP**. Excedido o limite, retorna `429 Too Many Requests`. O IP real é extraído do `X-Forwarded-For` apenas quando o `remoteAddr` da requisição está na lista `TRUSTED_PROXIES` (vazia por padrão), evitando spoofing de IP.

### Credenciais de seed (somente desenvolvimento)

| E-mail | Senha | Role |
|---|---|---|
| `admin@distribuidor.com` | `Admin123!` | `admin` |
| `gerente@distribuidor.com` | `Gerente123!` | `gerente_estoque` |
| `vendedor1@distribuidor.com` | `Vendedor123!` | `vendedor` |

> Documentação completa de auth, refresh e logout: [`docs/auth/README.md`](docs/auth/README.md)

---

## Endpoints

Todos os endpoints retornam JSON. Erros seguem o formato padrão:

```json
{ "error": "mensagem descritiva", "status": 400 }
```

Para referência completa com exemplos de request/response: [`docs/api/reference.md`](docs/api/reference.md)

---

### Autenticação — `/api/auth`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/login` | Público | Autentica e define cookie JWT |
| `POST` | `/api/auth/register` | ADMIN | Cria novo usuário |
| `POST` | `/api/auth/refresh` | Autenticado | Renova token e atualiza cookie |
| `POST` | `/api/auth/logout` | Autenticado | Limpa o cookie JWT |
| `GET` | `/api/auth/validate` | Autenticado | Valida token e retorna `{ valid, email, role }` |

---

### Clientes — `/api/customers`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| `POST` | `/api/customers` | Autenticado | Cria cliente; campo `dadosAdicionais` aceita JSONB livre |
| `GET` | `/api/customers` | Autenticado | Lista com paginação |
| `GET` | `/api/customers/{id}` | Autenticado | Detalhe + `totalPedidos`, `valorTotalGasto`, `ultimaCompra` |
| `PUT` | `/api/customers/{id}` | Autenticado | Atualiza dados cadastrais |
| `GET` | `/api/customers/{id}/orders` | Autenticado | Histórico de pedidos do cliente (paginado) |

O detalhe do cliente (`GET /{id}`) retorna o resumo consolidado via `vw_customer_summary`: apenas pedidos com status `RETIRADO` são contabilizados no `valorTotalGasto`.

> Documentação detalhada: [`docs/api/customers.md`](docs/api/customers.md)

---

### Produtos — `/api/products`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/products` | Autenticado | Lista produtos ativos (paginado) |
| `GET` | `/api/products/{id}` | Autenticado | Detalhe do produto |
| `POST` | `/api/products` | ADMIN | Cria produto e inicializa estoque em 0 |
| `PUT` | `/api/products/{id}` | ADMIN | Atualiza dados do produto |
| `DELETE` | `/api/products/{id}` | ADMIN | Desativa produto (soft delete — `ativo=false`) |

Produtos desativados não aparecem na listagem nem no estoque, mas são preservados no histórico de pedidos para integridade referencial.

> Documentação detalhada: [`docs/api/products-inventory.md`](docs/api/products-inventory.md)

---

### Estoque — `/api/inventory`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/inventory/status` | Autenticado | Todos os produtos com status `OK` / `BAIXO` / `CRÍTICO`, ordenados por severidade |
| `GET` | `/api/inventory/{productId}` | Autenticado | Status de um produto específico |

**Classificação de criticidade:**

| Status | Condição |
|---|---|
| `CRITICO` | `quantidadeAtual <= estoqueMinimo` |
| `BAIXO` | `quantidadeAtual <= estoqueMinimo × 1.5` |
| `OK` | Acima de 1.5× o mínimo |

---

### Movimentações de estoque — `/api/inventory/movements`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/inventory/movements` | Autenticado | Histórico com filtro por `productId` e paginação |

Movimentações são criadas automaticamente pelo sistema: `OUT` ao marcar pedido como `ATENDIDO`, `IN` ao processar uma devolução. Cada registro inclui `referenciaId` (id do pedido ou devolução) e `referenciaTipo` para rastreabilidade.

---

### Pedidos — `/api/orders`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| `POST` | `/api/orders` | VENDEDOR, ADMIN | Cria pedido em status `PENDENTE` |
| `GET` | `/api/orders` | Autenticado | Lista com filtros opcionais (paginado) |
| `GET` | `/api/orders/{id}` | Autenticado | Detalhe com itens |
| `PUT` | `/api/orders/{id}` | VENDEDOR, ADMIN | Atualiza itens e desconto (somente `PENDENTE`) |
| `DELETE` | `/api/orders/{id}` | VENDEDOR, ADMIN | Exclui pedido (somente `PENDENTE`) |
| `PATCH` | `/api/orders/{id}/status` | GERENTE_ESTOQUE, ADMIN | Avança para `ATENDIDO` ou `RETIRADO` |

**Filtros disponíveis em `GET /api/orders`:**

| Parâmetro | Tipo | Exemplo |
|---|---|---|
| `status` | enum | `PENDENTE`, `ATENDIDO`, `RETIRADO` |
| `customerId` | UUID | `?customerId=uuid` |
| `criadoPor` | UUID | `?criadoPor=uuid` |
| `dataInicio` / `dataFim` | ISO-8601 | `?dataInicio=2025-01-01T00:00:00` |
| `valorMin` / `valorMax` | decimal | `?valorMin=100&valorMax=5000` |

**Body de criação:**
```json
{
  "customerId": "uuid-do-cliente",
  "items": [
    { "productId": "uuid", "quantidade": 50 },
    { "productId": "uuid", "quantidade": 10 }
  ],
  "descontoAplicado": 5.00,
  "tipoDesconto": "PERCENTUAL",
  "notas": "Entrega urgente — UTI do Hospital Regional"
}
```

O preço unitário de cada item é congelado no momento da criação (snapshot do `precoBase` do produto).

> Documentação detalhada: [`docs/api/orders.md`](docs/api/orders.md)

---

### Devoluções — `/api/returns`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| `POST` | `/api/returns` | GERENTE_ESTOQUE, ADMIN | Abre devolução (somente pedidos `RETIRADO`) |
| `GET` | `/api/returns` | Autenticado | Lista devoluções (paginado) |
| `GET` | `/api/returns/{id}` | Autenticado | Detalhe da devolução |
| `PATCH` | `/api/returns/{id}/process` | GERENTE_ESTOQUE, ADMIN | Processa → reverte estoque com movimentação `IN` |
| `PATCH` | `/api/returns/{id}/reject` | GERENTE_ESTOQUE, ADMIN | Rejeita → sem alterar estoque |

A quantidade devolvida de cada item não pode exceder a quantidade original no pedido. O número de devolução é gerado automaticamente (`DEV-XXXXXX`).

---

### Comissões — `/api/commissions`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/commissions` | ADMIN | Lista comissões com filtro por `status` (`PENDENTE` / `PAGO`) |

O valor da comissão é calculado diretamente pelo banco: `valor_vendido × taxa_comissao / 100` (coluna `GENERATED STORED`).

---

### Performance de vendedores — `/api/sellers`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/sellers/performance` | VENDEDOR, ADMIN | Performance do usuário autenticado no mês corrente |
| `GET` | `/api/sellers/performance/all` | ADMIN | Performance de todos os vendedores ativos no mês |

Performance considera apenas pedidos com status `RETIRADO` no mês vigente, via view `vw_seller_performance_current_month`. Retorna `totalPedidos`, `valorVendido` e `quantidadeUnidades`.

---

### Usuários — `/api/users`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/users` | ADMIN | Lista todos os usuários (paginado) |
| `GET` | `/api/users/{id}` | ADMIN | Detalhe do usuário |
| `PATCH` | `/api/users/{id}` | ADMIN | Atualiza nome, telefone, role e status ativo |

> Criação de novos usuários é feita via `POST /api/auth/register` (requer ADMIN).

---

## Fluxo de negócio

```
                    ┌───────────┐
      VENDEDOR      │  PENDENTE │  ← POST /api/orders
      cria pedido   └─────┬─────┘
                          │
                          │  GERENTE marca ATENDIDO
                          │  → estoque decrementado (OUT)
                          │  → e-mail enviado ao cliente
                          ▼
                    ┌───────────┐
                    │  ATENDIDO │  ← PATCH /{id}/status
                    └─────┬─────┘
                          │
                          │  GERENTE confirma retirada
                          ▼
                    ┌───────────┐
                    │  RETIRADO │  ← PATCH /{id}/status
                    └─────┬─────┘
                          │
               (opcional) │  Devolução parcial ou total
                          ▼
                  ┌───────────────┐
                  │   DEVOLUÇÃO   │
                  │   PENDENTE    │
                  └───────┬───────┘
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
       ┌────────────┐         ┌────────────┐
       │ PROCESSADO │         │ REJEITADO  │
       │ estoque    │         │ estoque    │
       │ revertido  │         │ intacto    │
       └────────────┘         └────────────┘
```

**Regras do fluxo:**
- Pedidos só podem ser editados (`PUT`) ou excluídos (`DELETE`) enquanto `PENDENTE`.
- `PENDENTE → ATENDIDO` valida estoque disponível antes de decrementar. Estoque insuficiente retorna `400`.
- Não é possível regredir status (`RETIRADO → ATENDIDO` retorna `400`).
- Quantidade devolvida não pode exceder a do item original no pedido.
- O número do pedido (`PED-XXXXXX`) e da devolução (`DEV-XXXXXX`) são gerados por triggers no banco.

> Regras completas de negócio: [`docs/specs/04-business-rules.md`](docs/specs/04-business-rules.md)

---

## Notificações por e-mail

O sistema envia e-mails **após o commit da transação** via `TransactionSynchronizationManager.afterCommit()`. Falhas de SMTP são capturadas e logadas, mas nunca revertem a operação principal.

| Evento | Destinatários |
|---|---|
| Pedido criado (`PENDENTE`) | Todos os usuários com role `ADMIN` ou `GERENTE_ESTOQUE` ativos |
| Pedido marcado como `ATENDIDO` | Cliente do pedido + todos os `ADMIN` e `GERENTE_ESTOQUE` ativos |

> Configuração SMTP: [`docs/ferramentas/smtp-gmail.md`](docs/ferramentas/smtp-gmail.md)  
> Decisão técnica (e-mail fora da transação): [`docs/decisoes-tecnicas/0005-email-fora-da-transacao-de-pedido.md`](docs/decisoes-tecnicas/0005-email-fora-da-transacao-de-pedido.md)

---

## Banco de dados

### Migrações Flyway (16 versões)

| Versão | Conteúdo |
|---|---|
| V1 | Tabela `users` + trigger `updated_at` |
| V2 | Seed de usuários de desenvolvimento |
| V3 | Tabela `products` |
| V4 | Tabela `inventory` (OneToOne com product) |
| V5 | View `vw_inventory_status` |
| V6 | Seed de 5 produtos e estoque inicial |
| V7 | Tabela `customers` (com coluna `dados_adicionais JSONB`) |
| V8 | Tabela `inventory_movements` |
| V9 | Tabela `orders` + sequence + trigger de numeração |
| V10 | Tabela `order_items` (coluna `subtotal GENERATED STORED`) |
| V11 | Tabela `returns` + sequence + trigger de numeração |
| V12 | Tabela `return_items` (coluna `subtotal GENERATED STORED`) |
| V13 | Tabela `commissions` (coluna `valor_comissao GENERATED STORED`) |
| V14 | View `vw_seller_performance_current_month` |
| V15 | View `vw_customer_summary` |
| V16 | Coluna `retirado_por` em `orders` |

### Views de relatório (somente leitura)

| View | Usado por |
|---|---|
| `vw_seller_performance_current_month` | `GET /api/sellers/performance` — pedidos RETIRADO no mês atual |
| `vw_customer_summary` | `GET /api/customers/{id}` — total de pedidos e valor gasto |

> Esquema completo de banco: [`docs/db/README.md`](docs/db/README.md)

---

## Testes e cobertura

```bash
# Rodar todos os testes
./gradlew test

# Gerar relatório HTML de cobertura
./gradlew jacocoTestReport
# Relatório em: build/reports/jacoco/test/html/index.html

# Verificar cobertura mínima de 80% (bloqueia build se falhar)
./gradlew jacocoTestCoverageVerification
```

O banco PostgreSQL precisa estar rodando antes dos testes de integração:

```bash
docker compose up postgres -d
```

Os testes de integração sobem o contexto Spring completo com `@SpringBootTest + @AutoConfigureMockMvc + @Transactional`. Cada endpoint é testado com token JWT real extraído do cookie `Set-Cookie`, cobrindo fluxos de sucesso, erros de validação (400), autenticação (401), autorização (403) e recursos inexistentes (404).

O CI (GitHub Actions) executa `build + test + jacocoTestCoverageVerification` em todo PR para `main` e `develop`. Merge bloqueado automaticamente se qualquer etapa falhar.

> Detalhes do JaCoCo e métricas: [`docs/ferramentas/jacoco.md`](docs/ferramentas/jacoco.md)  
> Pipeline de CI: [`docs/ferramentas/github-actions.md`](docs/ferramentas/github-actions.md)

---

## Decisões técnicas

**JWT via cookie HttpOnly** — o token não aparece na resposta JSON nem pode ser lido por JavaScript, eliminando o risco de XSS roubar o token. Clientes de API usam `Authorization: Bearer` normalmente. Logout limpa o cookie no servidor. Ver [`docs/auth/README.md`](docs/auth/README.md).

**Rate limiting no IP real** — o `X-Forwarded-For` só é confiado se o `remoteAddr` estiver na lista `TRUSTED_PROXIES` (vazia por padrão). Previne que um atacante forje o IP de origem e bypass o limite de tentativas de login.

**Verificação de `ativo` por requisição** — o filtro JWT consulta o banco a cada request para verificar `ativo=true`. Desativar um usuário via `PATCH /api/users/{id}` surte efeito imediato, sem precisar aguardar expiração do token. Custo: uma leitura por PK, negligenciável.

**E-mail assíncrono pós-commit** — notificações são disparadas após o commit via `TransactionSynchronizationManager.afterCommit()`. Falha de SMTP não reverte a transação nem bloqueia o fluxo de pedidos. Ver [`docs/decisoes-tecnicas/0005-email-fora-da-transacao-de-pedido.md`](docs/decisoes-tecnicas/0005-email-fora-da-transacao-de-pedido.md).

**Soft delete em produtos** — `DELETE /api/products/{id}` seta `ativo=false`. Hard delete quebraria a integridade referencial com `order_items` e apagaria o histórico de pedidos.

**Estoque criado com o produto** — ao criar um produto, uma entrada em `inventory` com `quantidade=0` é persistida na mesma transação. Garante que todo produto tem registro de estoque desde o início.

**Colunas `GENERATED STORED`** — `order_items.subtotal`, `return_items.subtotal` e `commissions.valor_comissao` são calculadas pelo banco, garantindo consistência sem lógica duplicada na aplicação.

**Package-by-feature** — cada módulo é autocontido. `auth` não depende de `order`, `inventory` não conhece `customer`. Facilita extrair módulos como microsserviços no futuro. Ver [`docs/decisoes-tecnicas/`](docs/decisoes-tecnicas/).

---

## Swagger UI

Disponível apenas em desenvolvimento (desabilitado em produção via `application-prod.yml`).

```
http://localhost:8080/swagger-ui/index.html
```

Para autenticar no Swagger UI:
1. Execute `POST /api/auth/login` na interface
2. Na aba de resposta, veja o header `Set-Cookie: jwt=<token>;...`
3. Copie o valor do token (entre `jwt=` e `;`)
4. Clique em **Authorize** e cole como `Bearer <token>`

> Configuração do SpringDoc: [`docs/ferramentas/springdoc-openapi.md`](docs/ferramentas/springdoc-openapi.md)

---

## Documentação detalhada

### Ordem de leitura recomendada

Se você está chegando ao projeto pela primeira vez, leia na ordem abaixo. Cada documento assume que o anterior já foi lido.

| # | Documento | O que você vai entender |
|---|---|---|
| 1 | [`docs/specs/01-overview.md`](docs/specs/01-overview.md) | Contexto de negócio, problema resolvido e escopo do sistema |
| 2 | [`docs/specs/02-data-model.md`](docs/specs/02-data-model.md) | Entidades, relacionamentos e o modelo de dados completo |
| 3 | [`docs/specs/04-business-rules.md`](docs/specs/04-business-rules.md) | Regras por role, validações, fluxo de status e restrições |
| 4 | [`docs/auth/README.md`](docs/auth/README.md) | Como a autenticação JWT + cookie funciona na prática |
| 5 | [`docs/db/README.md`](docs/db/README.md) | Schema do banco, views, triggers e índices |
| 6 | [`docs/configuracao.md`](docs/configuracao.md) | Setup local, variáveis de ambiente e configuração de e-mail |
| 7 | [`docs/api/reference.md`](docs/api/reference.md) | Todos os endpoints com body, params e exemplos de response |

### Aprofundamento por tema

Após a leitura inicial, consulte conforme a necessidade:

| Tema | Documento |
|---|---|
| Endpoints de auth | [`docs/api/auth.md`](docs/api/auth.md) |
| Endpoints de pedidos | [`docs/api/orders.md`](docs/api/orders.md) |
| Endpoints de clientes | [`docs/api/customers.md`](docs/api/customers.md) |
| Endpoints de produtos e estoque | [`docs/api/products-inventory.md`](docs/api/products-inventory.md) |
| Por que Flyway e não Liquibase | [`docs/decisoes-tecnicas/0001-ferramenta-de-migration.md`](docs/decisoes-tecnicas/0001-ferramenta-de-migration.md) |
| Por que PostgreSQL | [`docs/decisoes-tecnicas/0002-banco-de-dados-dev.md`](docs/decisoes-tecnicas/0002-banco-de-dados-dev.md) |
| Por que e-mail assíncrono | [`docs/decisoes-tecnicas/0005-email-fora-da-transacao-de-pedido.md`](docs/decisoes-tecnicas/0005-email-fora-da-transacao-de-pedido.md) |
| Estratégia de branching e CI/CD | [`docs/decisoes-tecnicas/0004-estrategia-de-branching-e-cicd.md`](docs/decisoes-tecnicas/0004-estrategia-de-branching-e-cicd.md) |
| Docker e Docker Compose | [`docs/ferramentas/docker.md`](docs/ferramentas/docker.md) |
| Flyway (migrations) | [`docs/ferramentas/flyway.md`](docs/ferramentas/flyway.md) |
| JaCoCo (cobertura) | [`docs/ferramentas/jacoco.md`](docs/ferramentas/jacoco.md) |
| GitHub Actions (CI) | [`docs/ferramentas/github-actions.md`](docs/ferramentas/github-actions.md) |
| Swagger / SpringDoc | [`docs/ferramentas/springdoc-openapi.md`](docs/ferramentas/springdoc-openapi.md) |
| Configuração de SMTP | [`docs/ferramentas/smtp-gmail.md`](docs/ferramentas/smtp-gmail.md) |
| Roadmap e histórias de usuário | [`docs/specs/05-roadmap.md`](docs/specs/05-roadmap.md) · [`docs/specs/06-user-stories.md`](docs/specs/06-user-stories.md) |
