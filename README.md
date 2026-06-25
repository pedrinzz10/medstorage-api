# MedStorage API

[![CI](https://github.com/pedrinzz10/medstorage-api/actions/workflows/ci.yml/badge.svg)](https://github.com/pedrinzz10/medstorage-api/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)

API REST para gestão de pedidos e estoque de um distribuidor de materiais médicos. Controla o ciclo completo: cadastro de clientes e produtos, controle de estoque, criação de pedidos e notificações por e-mail ao cliente quando o pedido fica pronto.

---

## Stack

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem principal |
| Spring Boot 4.1.0 | Framework web, DI, Security |
| PostgreSQL 16 | Banco de dados relacional |
| Flyway | Versionamento de schema (migrações SQL) |
| JWT (JJWT 0.12) | Autenticação stateless |
| SpringDoc OpenAPI 2.8 | Documentação interativa (Swagger UI) |
| Docker + Compose | Containerização e ambiente local |
| JaCoCo | Cobertura de testes (mínimo 80%) |
| GitHub Actions | CI/CD — build, testes e cobertura a cada push |

---

## Arquitetura

O projeto segue **package-by-feature** com sub-pacotes por camada dentro de cada módulo:

```
src/main/java/.../
├── auth/
│   ├── controller/   AuthController
│   ├── service/      AuthService
│   └── dto/
├── customer/
│   ├── controller/   CustomerController
│   ├── service/      CustomerService
│   ├── repository/   CustomerRepository
│   ├── entity/       Customer
│   └── dto/
├── product/          (mesma estrutura)
├── inventory/        (mesma estrutura)
├── inventorymovement/
│   ├── entity/       InventoryMovement
│   ├── repository/
│   └── enums/        MovementType
├── order/
│   ├── controller/   OrderController
│   ├── service/      OrderService, OrderNotificationService
│   ├── repository/   OrderRepository, OrderSpecifications
│   ├── entity/       Order, OrderItem
│   ├── enums/        OrderStatus
│   └── dto/
├── user/
│   ├── entity/       User
│   ├── repository/
│   └── enums/        UserRole, UserRoleConverter
├── security/         JwtProvider, JwtAuthenticationFilter, SecurityConfig
├── exception/        GlobalExceptionHandler, exceções customizadas
└── config/           OpenApiConfig
```

**Fluxo de pedido:**
`POST /api/orders` → reserva estoque → `PATCH status → ATENDIDO` → baixa estoque + e-mail → `PATCH status → RETIRADO`

---

## Como rodar

### Pré-requisitos
- Docker e Docker Compose

### Opção 1 — Docker Compose completo (recomendado)

```bash
# 1. Clone o repositório
git clone https://github.com/pedrinzz10/medstorage-api.git
cd medstorage-api

# 2. Configure as variáveis de ambiente
cp .env.example .env
# Edite .env se quiser — os valores padrão já funcionam localmente

# 3. Suba tudo
docker-compose up --build
```

A API ficará disponível em `http://localhost:8080`.

### Opção 2 — Desenvolvimento local (hot reload)

```bash
# 1. Suba só o banco
docker-compose up postgres -d

# 2. Configure o ambiente
cp .env.example .env

# 3. Execute a aplicação
./gradlew bootRun
```

> Requer Java 21 instalado localmente.

---

## Endpoints

### Autenticação

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| POST | `/api/auth/login` | Login → retorna JWT | Pública |
| POST | `/api/auth/register` | Cadastra usuário | ADMIN |
| GET | `/api/auth/validate` | Valida token | Autenticado |
| POST | `/api/auth/refresh` | Renova token | Autenticado |

### Clientes

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| POST | `/api/customers` | Criar cliente | Autenticado |
| GET | `/api/customers` | Listar (paginado) | Autenticado |
| GET | `/api/customers/{id}` | Buscar por ID | Autenticado |
| PUT | `/api/customers/{id}` | Atualizar | Autenticado |

### Produtos

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| GET | `/api/products` | Listar ativos (paginado) | Autenticado |
| GET | `/api/products/{id}` | Buscar por ID | Autenticado |

### Estoque

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| GET | `/api/inventory/status` | Status de todos os produtos | Autenticado |
| GET | `/api/inventory/{productId}` | Status de um produto | Autenticado |

### Pedidos

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| POST | `/api/orders` | Criar pedido | VENDEDOR, ADMIN |
| GET | `/api/orders` | Listar com filtros (paginado) | Autenticado |
| GET | `/api/orders/{id}` | Buscar por ID | Autenticado |
| PATCH | `/api/orders/{id}/status` | Mudar status (ATENDIDO/RETIRADO) | GERENTE_ESTOQUE, ADMIN |

---

## Swagger UI

Acesse a documentação interativa em:

```
http://localhost:8080/swagger-ui.html
```

Para autenticar no Swagger UI:
1. Faça login via `POST /api/auth/login` e copie o `token` da resposta
2. Clique em **Authorize** (canto superior direito)
3. Cole o token no campo `bearerAuth` e confirme

---

## Credenciais de seed

A migration `V2__seed_dev_users.sql` cria 3 usuários para teste local:

| E-mail | Senha | Papel |
|---|---|---|
| admin@distribuidor.com | Admin123! | admin |
| gerente@distribuidor.com | Gerente123! | gerente_estoque |
| vendedor1@distribuidor.com | Vendedor123! | vendedor |

> Nunca usar em produção.

---

## Testes

```bash
# Rodar todos os testes (requer Docker para subir PostgreSQL via Testcontainers)
./gradlew test

# Verificar cobertura mínima de 80%
./gradlew jacocoTestCoverageVerification

# Relatório HTML em: build/reports/jacoco/test/html/index.html
```

O projeto tem **71 testes**: unitários com Mockito (services) e integração com MockMvc + PostgreSQL real.

---

## Decisões técnicas

- **Package-by-feature com sub-pacotes** — cada módulo tem `entity/`, `service/`, `repository/`, `controller/` e `dto/` próprios, tornando fácil localizar qualquer arquivo sem conhecer o projeto inteiro.
- **JWT stateless** — sem sessão no servidor. O token carrega `userId`, `email` e `role`; o logout é client-side (descartar o token). Simplifica escalonamento horizontal.
- **Flyway para migrações** — cada alteração de schema é um arquivo SQL versionado em `src/main/resources/db/migration/`. O banco nunca é recriado; apenas as migrações pendentes são aplicadas.
- **Notificação por e-mail fora da transação** — o envio de e-mail ao marcar um pedido como ATENDIDO é registrado via `TransactionSynchronizationManager.afterCommit()`, garantindo que falha de SMTP nunca reverta uma transação de negócio.
- **JaCoCo 80%** — cobertura mínima enforced no CI. O build falha se cair abaixo disso.
