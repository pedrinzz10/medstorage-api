# 0001 — Ferramenta de migration de banco de dados

## Contexto
O schema do banco (10 tabelas, views, triggers) precisa ser versionado e aplicado de forma consistente entre ambiente local, staging e produção, sem depender de execução manual de scripts SQL.

## Decisão
Usar **Flyway** (`flyway-core` + `flyway-database-postgresql`), com scripts SQL versionados em `src/main/resources/db/migration`, seguindo a convenção `V{numero}__{descricao}.sql`.

## Alternativas consideradas
- **Liquibase** — mais flexível (suporta XML/YAML/JSON além de SQL), mas adiciona complexidade desnecessária para um schema relacional simples como este.
- **`schema.sql`/`data.sql` do Spring** — mais simples de configurar, mas não versiona migrations de forma incremental; arriscado conforme o schema evolui (ex.: não dá pra saber o que já foi aplicado em produção).

## Consequências
- Cada mudança de schema (nova tabela, coluna, índice) exige um novo arquivo de migration, nunca editar um já aplicado.
- O Flyway mantém uma tabela `flyway_schema_history` no próprio banco para rastrear o que já rodou.
- Documentação de uso/aprendizado em `docs/ferramentas/flyway.md`.
- **Achado na Sprint 1:** no Spring Boot 4.1, `flyway-core` + `flyway-database-postgresql` sozinhos não bastam — é necessário também `org.springframework.boot:spring-boot-flyway`, módulo que carrega a autoconfiguração (extraída do `spring-boot-autoconfigure` core a partir do Boot 4). Sem ele, o Flyway não roda e nenhum erro é lançado. Detalhes em `docs/ferramentas/flyway.md`.
