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
- Java 21, Spring Boot 4.1.0, Gradle
- spring-boot-starter-data-jpa, -security, -validation, -webmvc
- PostgreSQL (runtime), Lombok
- Testes: starters `*-test` equivalentes (JUnit 5)

> Nota: a documentação original menciona "Spring Boot 3" — o projeto real já está em **Spring Boot 4.1.0**. Specs e exemplos de código devem ser validados contra essa versão (ex.: pacotes de segurança podem ter mudado).

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
- Cobertura de testes ≥ 80% por sprint
- Zero warnings/erros no build
- Documentação de API via OpenAPI/Swagger (`springdoc-openapi`)
- Auditoria: toda ação relevante registra quem fez e quando
