# 01. Visão Geral — MedStorage API

## Produto
SaaS de gestão de pedidos para distribuidor de materiais médicos (hospitais/clínicas como clientes).

## Problema
- Rastreamento de pedidos em tempo real
- Visibilidade compartilhada entre vendedores
- Integração com controle de estoque
- Medição de performance e comissões dos vendedores
- Notificação de clientes sobre status dos pedidos

## Stack confirmada no repositório
- **Java 21**, Spring Boot 4.1.0, Gradle
- spring-boot-starter-data-jpa, -security, -validation, -webmvc, -mail
- PostgreSQL 16 (runtime), Lombok
- **Flyway** — migrações versionadas em `src/main/resources/db/migration` (V1–V10)
- **JJWT 0.12.6** — geração e validação de tokens JWT (HS384)
- **SpringDoc OpenAPI 2.8.5** — Swagger UI em `/swagger-ui.html`
- **Docker + Compose** — `Dockerfile` multi-stage (JDK 21 → JRE 21 Alpine) + `docker-compose.yml` com serviços `postgres` e `app`
- **JaCoCo** — cobertura mínima de 80% enforced no build
- **GitHub Actions** — CI em `.github/workflows/ci.yml` (build + testes + cobertura a cada push/PR)
- Testes: JUnit 5, Mockito, MockMvc, integração com PostgreSQL real (Testcontainers implícito via `@SpringBootTest`)

## Arquitetura de pacotes
Package-by-feature com sub-pacotes por camada dentro de cada módulo:
`entity/`, `repository/`, `service/`, `controller/`, `dto/`, `enums/`
(ex.: `order/entity/Order.java`, `order/service/OrderService.java`).
Módulos: `auth`, `customer`, `product`, `inventory`, `inventorymovement`, `order`, `user`.
Pacotes cross-cutting: `security/`, `exception/`, `common/`, `config/`.

## Escopo do MVP (Fase 1)
- Vendedor cria pedidos manualmente
- Gerente de estoque marca pedido como ATENDIDO / RETIRADO
- Estoque decrementa/incrementa automaticamente
- Performance de vendedores (valor, quantidade, comissão)
- Email de notificação quando pedido está pronto
- Devoluções com reversão de estoque

## Fora do escopo (futuro)
- Portal web para clientes criarem pedidos próprios
- App mobile
- Integração com nota fiscal eletrônica (NF-e)
- Dashboards analíticos avançados / relatórios PDF

## Papéis e permissões

| Ação | Vendedor | Gerente Estoque | Admin |
|---|---|---|---|
| Ver todos os pedidos | ✓ | ✓ | ✓ |
| Criar pedido | ✓ | ✗ | ✓ |
| Marcar ATENDIDO | ✗ | ✓ | ✓ |
| Marcar RETIRADO | ✗ | ✓ | ✓ |
| Registrar devolução | ✗ | ✓ | ✓ |
| Ver performance | ✓ (própria) | ✗ | ✓ (todos) |
| Gerenciar estoque | ✗ | ✓ | ✓ |
| Gerenciar usuários | ✗ | ✗ | ✓ |

Papel `cliente` é reservado para uma fase futura (portal externo) e não deve ser implementado no MVP.

## Critérios de aceitação gerais (todo o projeto)
- Testes unitários (JUnit) em toda regra de negócio
- Testes de integração (Spring `@SpringBootTest` + MockMvc) em toda API
- Cobertura de testes ≥ 80% por sprint (enforced via JaCoCo no CI)
- Zero warnings/erros no build
- Documentação de API via OpenAPI/Swagger (`springdoc-openapi`) — **implementado** com anotações `@Tag`, `@Operation`, `@ApiResponse` nos controllers
- Auditoria: toda ação relevante registra quem fez e quando
