# MedStorage API

[![CI](https://github.com/pedrinzz10/medstorage-api/actions/workflows/ci.yml/badge.svg)](https://github.com/pedrinzz10/medstorage-api/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue?logo=openjdk)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)
![JaCoCo 80%+](https://img.shields.io/badge/Coverage-80%25%2B-brightgreen)

API REST para gestão de estoque e pedidos de uma distribuidora de materiais médicos. Permite que vendedores registrem pedidos, gerentes de estoque atendam e controlem as saídas de produtos, e administradores gerenciem o catálogo e os usuários do sistema.

---

## Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Stack e dependências](#stack-e-dependências)
- [Arquitetura](#arquitetura)
- [Como rodar](#como-rodar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Credenciais de seed](#credenciais-de-seed)
- [Endpoints](#endpoints)
- [Fluxo de um pedido](#fluxo-de-um-pedido)
- [Controle de acesso](#controle-de-acesso)
  - [Hardening de segurança](#hardening-de-segurança)
- [Banco de dados](#banco-de-dados)
- [Testes e cobertura](#testes-e-cobertura)
- [Decisões técnicas](#decisões-técnicas)

---

## Sobre o projeto

A MedStorage API resolve o ciclo completo de venda e distribuição de materiais médicos:

1. **Vendedor** registra um pedido vinculando cliente, produtos e desconto opcional.
2. **Gerente de estoque** aprova o pedido: o estoque é baixado automaticamente e o cliente recebe uma notificação por e-mail.
3. **Cliente retira** o pedido na distribuidora, encerrando o ciclo.

O sistema também expõe relatórios consolidados — resumo por cliente, desempenho de vendedor e alertas de estoque crítico — todos disponíveis via API sem consulta ad-hoc ao banco.

---

## Stack e dependências

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Persistência | Spring Data JPA + Hibernate |
| Banco de dados | PostgreSQL 16 |
| Migrações | Flyway 10 |
| Autenticação | JWT stateless (JJWT 0.12.6) |
| Validação | Jakarta Validation (Bean Validation 3.1) |
| Documentação | SpringDoc OpenAPI 2.8.5 (Swagger UI) |
| Notificação | Spring Mail (SMTP/Gmail) |
| Build | Gradle 8 |
| Testes | JUnit 5 + Mockito + Spring MockMvc |
| Cobertura | JaCoCo 0.8.12 (mínimo 80% enforced no CI) |
| Containerização | Docker + Docker Compose |
| CI/CD | GitHub Actions |

---

## Arquitetura

Organização **package-by-feature**: cada módulo de negócio agrupa seus próprios `entity`, `dto`, `repository`, `service` e `controller`.

```
src/main/java/com/saas/MedStorage_api/
│
├── auth/           # Login, registro, validação e renovação de JWT
├── user/           # Gerenciamento de usuários (CRUD, papéis)
├── product/        # Catálogo de produtos com soft delete
├── inventory/      # Controle de estoque e alertas de criticidade
├── customer/       # Clientes com resumo de compras
├── order/          # Pedidos: criação, edição, fluxo de status
├── returns/        # Devoluções e rejeições
│
├── security/       # JwtProvider, filtros e SecurityConfig
├── exception/      # GlobalExceptionHandler + ApiError padronizado
└── config/         # OpenAPI (Swagger), configurações gerais
```

### Camadas dentro de cada módulo

```
produto/
  entity/        → @Entity JPA
  dto/           → Records de entrada (Request) e saída (Response)
  repository/    → interface JpaRepository
  service/       → regras de negócio, @Transactional
  controller/    → @RestController com @PreAuthorize
  enums/         → tipos enumerados específicos do domínio
```

---

## Como rodar

### Pré-requisitos

- Docker e Docker Compose instalados
- Para desenvolvimento local: JDK 21

---

### Opção 1 — Docker Compose (recomendado)

Sobe banco e aplicação em um único comando:

```bash
# 1. Clone o repositório
git clone https://github.com/pedrinzz10/medstorage-api.git
cd medstorage-api

# 2. Crie o arquivo de variáveis de ambiente
cp .env.example .env
# Edite .env e defina JWT_SECRET (obrigatório)

# 3. Suba tudo
docker-compose up --build
```

A API ficará disponível em `http://localhost:8080`.
O Swagger UI estará em `http://localhost:8080/swagger-ui.html`.

---

### Opção 2 — Desenvolvimento local

```bash
# 1. Suba apenas o banco
docker-compose up postgres -d

# 2. Configure as variáveis de ambiente
export DB_NAME=medstorage
export DB_USER=medstorage
export DB_PASSWORD=medstorage
export JWT_SECRET=uma-chave-de-pelo-menos-32-caracteres-aqui

# 3. Execute a aplicação
./gradlew bootRun
```

O Flyway executa as migrações automaticamente ao iniciar. Os dados de seed (usuários e produtos de exemplo) são inseridos nas migrações `V2` e `V6`.

---

## Variáveis de ambiente

Crie um arquivo `.env` na raiz a partir do exemplo abaixo:

```dotenv
# Banco de dados
DB_NAME=medstorage
DB_USER=medstorage
DB_PASSWORD=medstorage
DB_PORT=5432

# JWT — obrigatório, mínimo 32 caracteres
JWT_SECRET=substitua-por-uma-chave-secreta-longa-e-aleatoria
JWT_EXPIRATION_MS=86400000   # 24 horas (padrão)

# CORS — origens permitidas (separadas por vírgula)
# Padrão em dev: http://localhost:3000,http://localhost:8080
CORS_ALLOWED_ORIGINS=https://app.distribuidor.com,https://admin.distribuidor.com

# E-mail (opcional — notificações ao atender pedido)
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=sua-senha-de-app-gmail
MAIL_FROM_NAME=MedStorage
```

> **Nota:** `JWT_SECRET` é obrigatório. Sem ele a aplicação não inicializa. As variáveis de e-mail são opcionais; se não configuradas, a notificação de pedido atendido é registrada em log mas não falha a operação.

> **Produção:** defina `SPRING_PROFILES_ACTIVE=prod` para desabilitar o Swagger UI e o endpoint `/v3/api-docs` automaticamente.

---

## Credenciais de seed

As migrações inserem três usuários de desenvolvimento prontos para uso:

| E-mail | Senha | Papel |
|---|---|---|
| `admin@distribuidor.com` | `Admin123!` | `admin` |
| `gerente@distribuidor.com` | `Gerente123!` | `gerente_estoque` |
| `vendedor1@distribuidor.com` | `Vendedor123!` | `vendedor` |

Também são inseridos 5 produtos de exemplo (luvas, seringas, máscaras, gaze e álcool) com estoque inicial de 1000 unidades cada.

> Nunca usar em produção.

---

## Endpoints

### Autenticação — `/api/auth`

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| POST | `/api/auth/login` | Autentica e retorna token JWT. Limitado a 5 tentativas/min por IP | Pública |
| POST | `/api/auth/register` | Cria novo usuário | ADMIN |
| GET | `/api/auth/validate` | Verifica se o token é válido e retorna dados do usuário | Token |
| POST | `/api/auth/refresh` | Reemite novo token a partir de um token ainda válido | Token |
| POST | `/api/auth/logout` | Logout orientado ao cliente (JWT stateless — sem invalidação no servidor) | Token |

**Exemplo de login:**
```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@distribuidor.com","password":"Admin123!"}' \
  | jq .token
```

Use o token retornado no header `Authorization: Bearer <token>` em todas as requisições protegidas.

---

### Produtos — `/api/products`

Catálogo de materiais médicos comercializados pela distribuidora.

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| GET | `/api/products` | Lista produtos ativos (paginado, default 20/página) | Token |
| GET | `/api/products/{id}` | Detalhe de um produto | Token |
| POST | `/api/products` | Cria produto e inicializa entrada de estoque zerada | ADMIN |
| PUT | `/api/products/{id}` | Atualiza dados do produto (nome, SKU, preço, unidade) | ADMIN |
| DELETE | `/api/products/{id}` | Desativa produto (soft delete — `ativo=false`) | ADMIN |

> Produtos desativados não aparecem na listagem nem no estoque, mas são preservados no histórico de pedidos (integridade referencial).

**Corpo de criação/atualização:**
```json
{
  "nome": "Luva Cirurgica Tamanho M",
  "descricao": "Luva estéril para procedimentos cirúrgicos",
  "sku": "LUV-M-001",
  "precoBase": 0.50,
  "unidade": "par",
  "estoqueMinimo": 100,
  "ativo": true
}
```

---

### Estoque — `/api/inventory`

Visão consolidada do estoque com classificação de criticidade calculada em tempo real.

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| GET | `/api/inventory/status` | Lista todos os produtos com quantidade e criticidade, ordenados por severidade | Token |
| GET | `/api/inventory/{productId}` | Status de estoque de um produto específico | Token |

**Classificação de criticidade:**

| Status | Condição |
|---|---|
| `CRITICO` | `quantidade_atual <= estoque_minimo` |
| `BAIXO` | `quantidade_atual <= estoque_minimo × 1.5` |
| `OK` | Acima de 1.5× o mínimo |

**Exemplo de resposta:**
```json
{
  "id": "uuid",
  "nome": "Luva Cirurgica Tamanho M",
  "sku": "LUV-M-001",
  "quantidadeAtual": 85,
  "estoqueMinimo": 100,
  "statusEstoque": "CRITICO"
}
```

---

### Clientes — `/api/customers`

Cadastro de hospitais, clínicas e distribuidores atendidos.

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| POST | `/api/customers` | Cadastra novo cliente | Token |
| GET | `/api/customers` | Lista clientes paginada | Token |
| GET | `/api/customers/{id}` | Detalhe do cliente + resumo de compras | Token |
| PUT | `/api/customers/{id}` | Atualiza dados cadastrais | Token |
| GET | `/api/customers/{id}/orders` | Histórico paginado de pedidos do cliente | Token |

O endpoint `GET /api/customers/{id}` retorna o resumo consolidado de compras via `vw_customer_summary`: total de pedidos realizados, valor total gasto e data da última compra.

---

### Pedidos — `/api/orders`

Núcleo operacional da distribuidora. Suporta criação, edição, exclusão e progressão de status.

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| POST | `/api/orders` | Cria pedido em status PENDENTE | VENDEDOR / ADMIN |
| GET | `/api/orders` | Lista pedidos com filtros opcionais (paginado) | Token |
| GET | `/api/orders/{id}` | Detalhe de um pedido | Token |
| PUT | `/api/orders/{id}` | Edita itens, desconto e notas (somente status PENDENTE) | VENDEDOR / ADMIN |
| DELETE | `/api/orders/{id}` | Exclui pedido (somente status PENDENTE) | VENDEDOR / ADMIN |
| PATCH | `/api/orders/{id}/status` | Avança o status do pedido (ATENDIDO ou RETIRADO) | GERENTE / ADMIN |

**Filtros disponíveis em `GET /api/orders`:**

| Parâmetro | Tipo | Exemplo |
|---|---|---|
| `status` | enum | `PENDENTE`, `ATENDIDO`, `RETIRADO` |
| `customerId` | UUID | `?customerId=uuid` |
| `criadoPor` | UUID | `?criadoPor=uuid` (filtra pelo vendedor responsável) |
| `dataInicio` / `dataFim` | ISO-8601 | `?dataInicio=2025-01-01T00:00:00` |
| `valorMin` / `valorMax` | decimal | `?valorMin=100&valorMax=5000` |

**Corpo de criação de pedido:**
```json
{
  "customerId": "uuid-do-cliente",
  "items": [
    { "productId": "uuid-do-produto", "quantidade": 50 },
    { "productId": "uuid-do-outro-produto", "quantidade": 10 }
  ],
  "descontoAplicado": 5.00,
  "tipoDesconto": "PERCENTUAL",
  "notas": "Entrega urgente — UTI do Hospital Regional"
}
```

---

### Usuários — `/api/users`

Gerenciamento de contas de acesso ao sistema. Exclusivo para administradores.

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| GET | `/api/users` | Lista todos os usuários (paginado) | ADMIN |
| GET | `/api/users/{id}` | Detalhe de um usuário | ADMIN |
| PATCH | `/api/users/{id}` | Atualiza nome, telefone, papel e status ativo/inativo | ADMIN |

> Criação de novos usuários é feita via `POST /api/auth/register` (também requer ADMIN).

---

## Fluxo de um pedido

```
[Vendedor]                    [Gerente de Estoque]         [Sistema]
    │                                  │                       │
    ├─ POST /api/orders ───────────────>                       │
    │  status: PENDENTE                │                       │
    │                                  │                       │
    │                                  ├─ PATCH /{id}/status ─>│
    │                                  │  newStatus: ATENDIDO  │
    │                                  │                       ├─ valida estoque
    │                                  │                       ├─ baixa quantidade
    │                                  │                       ├─ envia e-mail ao cliente
    │                                  │<──── 200 ATENDIDO ────┤
    │                                  │                       │
    │                                  ├─ PATCH /{id}/status ─>│
    │                                  │  newStatus: RETIRADO  │
    │                                  │<──── 200 RETIRADO ────┤
```

**Regras do fluxo:**
- Somente pedidos `PENDENTE` podem ser editados (`PUT`) ou excluídos (`DELETE`).
- A transição `PENDENTE → ATENDIDO` valida estoque disponível antes de baixar. Se insuficiente, retorna `400 Bad Request`.
- A notificação por e-mail é best-effort: falhas de SMTP são logadas mas não revertem a transação.
- Desconto máximo permitido: 50%.
- Pedidos só avançam de status — não é possível regredir (`RETIRADO → ATENDIDO`).

---

## Controle de acesso

Autenticação via **JWT stateless** com papel (`role`) embutido no token.

| Papel | Valor no token | Permissões |
|---|---|---|
| Admin | `admin` | Acesso total — gerencia usuários, produtos e pode executar qualquer operação |
| Gerente de estoque | `gerente_estoque` | Avança status de pedidos (`ATENDIDO` / `RETIRADO`), consulta estoque |
| Vendedor | `vendedor` | Cria, edita e exclui pedidos; consulta clientes, produtos e estoque |

**Rate limiting no login:** 5 tentativas por minuto por IP. Exceder retorna `429 Too Many Requests`.

### Hardening de segurança

| Proteção | Comportamento |
|---|---|
| Conta desativada | Login e qualquer requisição com token válido de usuário `ativo=false` são bloqueados imediatamente — o filtro JWT consulta o banco a cada requisição |
| Renovação de token | `POST /api/auth/refresh` busca o usuário no banco e reflete o papel e status atuais, evitando tokens com dados obsoletos |
| CORS | Somente as origens listadas em `CORS_ALLOWED_ORIGINS` são aceitas (sem wildcard) |
| Swagger em produção | Desabilitado automaticamente com `SPRING_PROFILES_ACTIVE=prod` |

### Autenticando no Swagger UI

1. Acesse `http://localhost:8080/swagger-ui.html`
2. Execute `POST /api/auth/login` com suas credenciais
3. Copie o valor do campo `token` da resposta
4. Clique no botão **Authorize** (cadeado no topo da página)
5. No campo `bearerAuth`, cole o token e confirme

---

## Banco de dados

### Migrações Flyway

| Versão | Descrição |
|---|---|
| V1 | Tabela `users` |
| V2 | Seed de usuários de desenvolvimento |
| V3 | Tabela `products` |
| V4 | Tabela `inventory` |
| V5 | View `vw_inventory_status` |
| V6 | Seed de produtos e estoque inicial |
| V7 | Tabela `customers` |
| V8 | Tabela `inventory_movements` |
| V9 | Tabela `orders` |
| V10 | Tabela `order_items` |
| V11 | Tabela `returns` |
| V12 | Tabela `return_items` |
| V13 | Tabela `commissions` |
| V14 | View `vw_seller_performance` |
| V15 | View `vw_customer_summary` |

### Views de relatório

| View | Uso via API |
|---|---|
| `vw_inventory_status` | Base para consultas manuais de estoque; o endpoint usa lógica Java para testabilidade |
| `vw_seller_performance` | Desempenho por vendedor: total de pedidos e volume faturado |
| `vw_customer_summary` | Resumo por cliente: total de pedidos, valor gasto e data da última compra (usada em `GET /api/customers/{id}`) |

---

## Testes e cobertura

```bash
# Rodar todos os testes
./gradlew test

# Gerar relatório de cobertura HTML
./gradlew jacocoTestReport
# Relatório em: build/reports/jacoco/test/html/index.html

# Verificar cobertura mínima de 80% (bloqueia o build se falhar)
./gradlew jacocoTestCoverageVerification
```

A suíte cobre dois níveis:

- **Testes unitários** com Mockito — services de todos os módulos testados isoladamente (sem banco, sem Spring context)
- **Testes de integração** com `@SpringBootTest + MockMvc + @Transactional` — cada endpoint testado com token JWT real, validações de payload, respostas de erro (400/401/403/404) e fluxos de ponta a ponta

O CI (GitHub Actions) roda `build + test + jacocoTestCoverageVerification` em todo PR para `main` e `develop`. O merge é bloqueado automaticamente se os testes ou a cobertura falharem.

---

## Decisões técnicas

**JWT stateless:** sem sessão no servidor. O token carrega `userId`, `email` e `role`. Logout é client-side (descarte do token). Renovação via `POST /api/auth/refresh` com token ainda válido. Revogação real exigiria uma blacklist (Redis), fora do escopo atual.

**Soft delete em produtos:** `DELETE /api/products/{id}` seta `ativo=false` em vez de remover o registro. Produtos estão referenciados em `order_items` — um hard delete quebraria a integridade referencial e apagaria o histórico de pedidos.

**Estoque criado junto com o produto:** ao criar um produto via `POST /api/products`, uma entrada em `inventory` com `quantidade=0` é persistida na mesma transação. Garante que todo produto tem um registro de estoque desde o primeiro momento, evitando 404 no endpoint de inventário.

**E-mail fora da transação de pedido:** a notificação ao cliente ao marcar um pedido como `ATENDIDO` ocorre após o commit via `TransactionSynchronizationManager.afterCommit()`. Falhas de SMTP são capturadas e logadas, mas nunca revertem a baixa de estoque. Instabilidade de e-mail não bloqueia operações críticas.

**Criticidade calculada em Java, não na view:** a view `vw_inventory_status` existe para consultas manuais, mas o endpoint `/api/inventory/status` recalcula a criticidade no `InventoryService`. Isso permite testar a lógica de classificação (`CRITICO` / `BAIXO` / `OK`) com testes unitários puros, sem depender de banco.

**Rate limiting em memória:** o controle de tentativas de login usa `ConcurrentHashMap` com janela deslizante de 1 minuto. Suficiente para instância única. Para múltiplas instâncias, seria necessário externalizar para Redis.

**Verificação de estado do usuário por requisição:** o filtro JWT consulta o banco a cada request autenticado para verificar se `ativo=true`. Isso garante que desativar um usuário via `PATCH /api/users/{id}` surta efeito imediato — sem precisar aguardar a expiração do token. O custo por requisição é uma leitura por chave primária (índice `users.id`), o que é negligenciável.

**CORS restrito via variável de ambiente:** em vez de `allowedOriginPatterns("*")`, as origens aceitas são lidas de `CORS_ALLOWED_ORIGINS`. O default em desenvolvimento inclui `localhost:3000` e `localhost:8080`; em produção a variável deve listar explicitamente cada domínio do frontend.

**Package-by-feature com sub-pacotes:** cada módulo de negócio é autocontido. `auth` não depende de `order`, `inventory` não conhece `customer`. Facilita extrair módulos em microsserviços no futuro sem grandes refatorações.
