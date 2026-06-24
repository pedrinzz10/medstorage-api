# Banco de Dados

Documentação viva do banco conforme migrations são aplicadas — histórico do que de fato existe no schema, distinto do planejamento.

## Referências de planejamento
- Modelo de dados completo planejado: `docs/specs/02-data-model.md`
- Como usar Flyway neste projeto: `docs/ferramentas/flyway.md`
- Como rodar o banco local: `docs/ferramentas/docker.md`

## Migrations aplicadas

| Versão | Arquivo | Descrição | Sprint |
|---|---|---|---|
| 1 | `V1__create_users_table.sql` | Tabela `users` (id, email, password_hash, nome, role, ativo, telefone) + índices em `email`/`role` + trigger de `updated_at` | Sprint 1 |
| 2 | `V2__seed_dev_users.sql` | 3 usuários de desenvolvimento (admin, gerente, vendedor) com senhas hash bcrypt — ver `docs/auth/README.md` | Sprint 1 |

Local dos arquivos: `src/main/resources/db/migration/`.

## Configuração de conexão (dev)
- `application.yml` lê `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` de variáveis de ambiente, com defaults iguais aos do `.env.example`
- `spring.jpa.hibernate.ddl-auto: validate` — o Hibernate **nunca** cria/altera tabelas; só o Flyway é responsável pelo schema
- `spring.flyway.locations: classpath:db/migration`

## Como verificar o estado do banco localmente
```bash
docker exec medstorage-postgres psql -U medstorage -d medstorage -c "\dt"
docker exec medstorage-postgres psql -U medstorage -d medstorage -c "select version, description, success from flyway_schema_history;"
```
