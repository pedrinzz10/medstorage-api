# 02. Modelo de Dados

Fonte: `docs/inicial_docs/SCHEMA_BANCO_DADOS.md` (consolidado e validado).

## Entidades e relacionamentos

```
users (1) ──→ (N) orders            (criado_por)
users (1) ──→ (N) inventory_movements (criado_por)
users (1) ──→ (N) commissions         (vendedor_id)
customers (1) ──→ (N) orders
orders (1) ──→ (N) order_items
orders (1) ──→ (N) returns
products (1) ──→ (N) order_items
products (1) ──→ (1) inventory
products (1) ──→ (N) inventory_movements
returns (1) ──→ (N) return_items
products (1) ──→ (N) return_items
```

## Tabelas

### users
- id UUID PK, email UNIQUE, password_hash, nome
- role: `vendedor` | `gerente_estoque` | `admin`
- ativo BOOLEAN, telefone, created_at, updated_at

### customers
- id UUID PK, nome, email, telefone, cnpj
- endereco, contato_principal, dados_adicionais JSONB
- created_at, updated_at

### products
- id UUID PK, nome, descricao, sku UNIQUE
- preco_base DECIMAL(10,2) (≥0), unidade, estoque_minimo (≥0), ativo
- created_at, updated_at

### inventory
- id UUID PK, product_id UNIQUE FK → products (CASCADE)
- quantidade INT (≥0), data_ultima_atualizacao, created_at, updated_at

### orders
- id UUID PK, numero_pedido UNIQUE (gerado: `PED-NNNNNN` via sequence)
- customer_id FK → customers (RESTRICT), criado_por FK → users (RESTRICT)
- status: `PENDENTE` | `ATENDIDO` | `RETIRADO` (transição única, não regride)
- valor_total, desconto_aplicado DECIMAL(10,2) (≥0), tipo_desconto
- notas, data_atendimento, data_retirada, created_at, updated_at
- CHECK de consistência: PENDENTE → ambas datas nulas; ATENDIDO → só data_atendimento; RETIRADO → ambas preenchidas

### order_items
- id UUID PK, order_id FK → orders (CASCADE), product_id FK → products (RESTRICT)
- quantidade INT (>0), preco_unitario DECIMAL(10,2) (≥0)
- subtotal = quantidade * preco_unitario (coluna gerada)

### inventory_movements
- id UUID PK, product_id FK → products (RESTRICT)
- tipo: `IN` | `OUT`, quantidade INT (>0)
- motivo (venda, devolução, ajuste), referencia_id, referencia_tipo (`order`|`return`)
- criado_por FK → users (SET NULL), created_at

### returns
- id UUID PK, order_id FK → orders (CASCADE)
- status: `PENDENTE` | `PROCESSADO` | `REJEITADO`
- data_solicitacao, data_processamento, processado_por FK → users (SET NULL)
- motivo, created_at, updated_at
- Regra: só pode ser criada para pedidos com status `RETIRADO`

### return_items
- id UUID PK, return_id FK → returns (CASCADE), product_id FK → products (RESTRICT)
- quantidade INT (>0), created_at

### commissions
- id UUID PK, vendedor_id FK → users (CASCADE)
- periodo_inicio/fim DATE, total_pedidos, valor_vendido, quantidade_unidades
- taxa_comissao DECIMAL(5,2) (0–100), valor_comissao (coluna gerada = valor_vendido * taxa/100)
- status: `PENDENTE` | `PAGO`

## Views úteis
- `vw_customer_summary` — total de pedidos, gasto total, última compra por cliente
- `vw_seller_performance_current_month` — pedidos/valor/unidades por vendedor no mês atual
- `vw_inventory_status` — status `OK` / `BAIXO` / `CRÍTICO` por produto

## Triggers/funções
- `update_updated_at_column()` — atualiza `updated_at` em todas as tabelas com esse campo
- `generate_order_number()` — gera `numero_pedido` via sequence `order_numero_seq` (start 1000)

## Índices recomendados
- `orders(status, created_at DESC)`, `orders(customer_id, status)`, `orders(criado_por, created_at DESC)`
- `order_items(order_id, product_id)`
- `inventory_movements(product_id, created_at DESC)`

## DDL completo
O DDL executável está distribuído nas migrations Flyway em `src/main/resources/db/migration/`:
- V1–V2: users (schema + seed de usuários de dev)
- V3–V6: products, inventory, view de status, seed de produtos/inventário
- V7: customers
- V8: inventory_movements
- V9–V10: orders, order_items

Tabelas futuras (returns, return_items, commissions) serão adicionadas nas migrations de Sprint 6 e 7. O DDL de referência completo está em `docs/inicial_docs/SCHEMA_BANCO_DADOS.md`.
