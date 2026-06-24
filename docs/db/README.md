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
| 3 | `V3__create_products_table.sql` | Tabela `products` + índices (`sku`, `nome`, `ativo`) + trigger de `updated_at` | Sprint 2 |
| 4 | `V4__create_inventory_table.sql` | Tabela `inventory` (1:1 com `products`) + trigger de `updated_at` | Sprint 2 |
| 5 | `V5__create_inventory_status_view.sql` | View `vw_inventory_status` (documentação/consulta manual — a API não usa esta view, calcula o status em Java) | Sprint 2 |
| 6 | `V6__seed_products_and_inventory.sql` | 5 produtos de exemplo + 1000 unidades de estoque cada | Sprint 2 |
| 7 | `V7__create_customers_table.sql` | Tabela `customers` (com `dados_adicionais JSONB`) + índices (`nome`, `email`, `cnpj`) + trigger de `updated_at` | Sprint 3 |
| 8 | `V8__create_inventory_movements_table.sql` | Tabela `inventory_movements` (histórico de IN/OUT) + índices | Sprint 4 |
| 9 | `V9__create_orders_table.sql` | Tabela `orders` + sequence/trigger de geração de `numero_pedido` + CHECK de workflow de status + índices | Sprint 4 |
| 10 | `V10__create_order_items_table.sql` | Tabela `order_items` (subtotal como coluna gerada) + índices | Sprint 4 |

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
