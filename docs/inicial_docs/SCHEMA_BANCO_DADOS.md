# Schema do Banco de Dados - SaaS Distribuidor Materiais Médicos

## 1. DIAGRAMA ERD (Entidade-Relacionamento)

```
┌─────────────────┐          ┌──────────────────┐
│     USERS       │          │   CUSTOMERS      │
├─────────────────┤          ├──────────────────┤
│ id (PK)         │          │ id (PK)          │
│ email (UNIQUE)  │          │ nome             │
│ password_hash   │          │ email            │
│ nome            │          │ telefone         │
│ role            │          │ cnpj             │
│ ativo           │          │ endereco         │
│ telefone        │          │ contato_principal│
│ created_at      │          │ dados_adicionais │
│ updated_at      │          │ created_at       │
└─────────────────┘          │ updated_at       │
        │                     └──────────────────┘
        │                              │
        │ criado_por (1:N)             │
        │                              │ (1:N) customer_id
        ▼                              ▼
┌──────────────────────────────────────────────┐
│            ORDERS (Pedidos)                  │
├──────────────────────────────────────────────┤
│ id (PK)                                      │
│ numero_pedido (UNIQUE)                       │
│ customer_id (FK) ────────────────────→ customers
│ criado_por (FK) ──────────────────→ users
│ status (PENDENTE|ATENDIDO|RETIRADO)         │
│ valor_total                                  │
│ desconto_aplicado                            │
│ tipo_desconto                                │
│ notas                                        │
│ data_atendimento                             │
│ data_retirada                                │
│ created_at                                   │
│ updated_at                                   │
└──────────────────────────────────────────────┘
        │ (1:N) order_id
        ▼
┌─────────────────────────────────┐
│      ORDER_ITEMS                │
├─────────────────────────────────┤
│ id (PK)                         │
│ order_id (FK) → orders          │
│ product_id (FK) → products      │
│ quantidade                      │
│ preco_unitario                  │
│ subtotal                        │
│ created_at                      │
└─────────────────────────────────┘


┌──────────────────────┐
│     PRODUCTS         │
├──────────────────────┤
│ id (PK)              │
│ nome                 │
│ descricao            │
│ sku (UNIQUE)         │
│ preco_base           │
│ unidade              │
│ estoque_minimo       │
│ ativo                │
│ created_at           │
│ updated_at           │
└──────────────────────┘
        │ (1:1)
        ▼
┌──────────────────────┐
│     INVENTORY        │
├──────────────────────┤
│ id (PK)              │
│ product_id (FK)      │
│ quantidade           │
│ data_atualizacao     │
│ created_at           │
│ updated_at           │
└──────────────────────┘

        │ (1:N) product_id
        ▼
┌──────────────────────────────────┐
│   INVENTORY_MOVEMENTS            │
├──────────────────────────────────┤
│ id (PK)                          │
│ product_id (FK) → products       │
│ tipo (IN|OUT)                    │
│ quantidade                       │
│ motivo (venda|devolução|ajuste)  │
│ referencia_id (pedido|devolução) │
│ criado_por (FK) → users          │
│ created_at                       │
└──────────────────────────────────┘


┌──────────────────────┐
│      RETURNS         │
├──────────────────────┤
│ id (PK)              │
│ order_id (FK)        │
│ status               │
│ data_solicitacao     │
│ data_processamento   │
│ processado_por (FK)  │
│ motivo               │
│ created_at           │
│ updated_at           │
└──────────────────────┘
        │ (1:N) return_id
        ▼
┌──────────────────────┐
│   RETURN_ITEMS       │
├──────────────────────┤
│ id (PK)              │
│ return_id (FK)       │
│ product_id (FK)      │
│ quantidade           │
│ created_at           │
└──────────────────────┘


┌──────────────────────────┐
│    COMMISSIONS           │
├──────────────────────────┤
│ id (PK)                  │
│ vendedor_id (FK) → users │
│ periodo_inicio           │
│ periodo_fim              │
│ total_pedidos            │
│ valor_vendido            │
│ quantidade_unidades      │
│ taxa_comissao (%)        │
│ valor_comissao           │
│ status (PENDENTE|PAGO)   │
│ created_at               │
│ updated_at               │
└──────────────────────────┘
```

---

## 2. CRIAÇÃO DE TABELAS (SQL PostgreSQL)

### 2.1 USERS (Usuários)

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nome VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL CHECK (role IN ('vendedor', 'gerente_estoque', 'admin')),
  ativo BOOLEAN DEFAULT true,
  telefone VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
```

---

### 2.2 CUSTOMERS (Clientes)

```sql
CREATE TABLE customers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nome VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  telefone VARCHAR(20),
  cnpj VARCHAR(18),
  endereco TEXT,
  contato_principal VARCHAR(255),
  dados_adicionais JSONB DEFAULT '{}',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_nome ON customers(nome);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_cnpj ON customers(cnpj);
```

---

### 2.3 PRODUCTS (Produtos/Tabela de Preços)

```sql
CREATE TABLE products (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nome VARCHAR(255) NOT NULL,
  descricao TEXT,
  sku VARCHAR(100) UNIQUE NOT NULL,
  preco_base DECIMAL(10, 2) NOT NULL CHECK (preco_base >= 0),
  unidade VARCHAR(50) DEFAULT 'unidade', -- unidade, caixa, pacote, etc
  estoque_minimo INTEGER DEFAULT 0 CHECK (estoque_minimo >= 0),
  ativo BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_nome ON products(nome);
CREATE INDEX idx_products_ativo ON products(ativo);
```

---

### 2.4 INVENTORY (Estoque)

```sql
CREATE TABLE inventory (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID UNIQUE NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  quantidade INTEGER NOT NULL DEFAULT 0 CHECK (quantidade >= 0),
  data_ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_product_id ON inventory(product_id);
```

---

### 2.5 ORDERS (Pedidos)

```sql
CREATE TABLE orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  numero_pedido VARCHAR(50) UNIQUE NOT NULL,
  customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
  criado_por UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDENTE' 
    CHECK (status IN ('PENDENTE', 'ATENDIDO', 'RETIRADO')),
  valor_total DECIMAL(10, 2) NOT NULL CHECK (valor_total >= 0),
  desconto_aplicado DECIMAL(10, 2) DEFAULT 0 CHECK (desconto_aplicado >= 0),
  tipo_desconto VARCHAR(100),
  notas TEXT,
  data_atendimento TIMESTAMP,
  data_retirada TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_numero ON orders(numero_pedido);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_criado_por ON orders(criado_por);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
```

---

### 2.6 ORDER_ITEMS (Itens do Pedido)

```sql
CREATE TABLE order_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
  quantidade INTEGER NOT NULL CHECK (quantidade > 0),
  preco_unitario DECIMAL(10, 2) NOT NULL CHECK (preco_unitario >= 0),
  subtotal DECIMAL(10, 2) GENERATED ALWAYS AS (quantidade * preco_unitario) STORED,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
```

---

### 2.7 INVENTORY_MOVEMENTS (Histórico de Movimentos)

```sql
CREATE TABLE inventory_movements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
  tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('IN', 'OUT')),
  quantidade INTEGER NOT NULL CHECK (quantidade > 0),
  motivo VARCHAR(255) NOT NULL,
  referencia_id UUID, -- pode ser um order_id ou return_id
  referencia_tipo VARCHAR(50), -- 'order' ou 'return'
  criado_por UUID NOT NULL REFERENCES users(id) ON DELETE SET NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movements_product_id ON inventory_movements(product_id);
CREATE INDEX idx_movements_tipo ON inventory_movements(tipo);
CREATE INDEX idx_movements_created_at ON inventory_movements(created_at);
```

---

### 2.8 RETURNS (Devoluções)

```sql
CREATE TABLE returns (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDENTE'
    CHECK (status IN ('PENDENTE', 'PROCESSADO', 'REJEITADO')),
  data_solicitacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  data_processamento TIMESTAMP,
  processado_por UUID REFERENCES users(id) ON DELETE SET NULL,
  motivo TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_returns_order_id ON returns(order_id);
CREATE INDEX idx_returns_status ON returns(status);
```

---

### 2.9 RETURN_ITEMS (Itens de Devolução)

```sql
CREATE TABLE return_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  return_id UUID NOT NULL REFERENCES returns(id) ON DELETE CASCADE,
  product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
  quantidade INTEGER NOT NULL CHECK (quantidade > 0),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_return_items_return_id ON return_items(return_id);
CREATE INDEX idx_return_items_product_id ON return_items(product_id);
```

---

### 2.10 COMMISSIONS (Comissões)

```sql
CREATE TABLE commissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendedor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  periodo_inicio DATE NOT NULL,
  periodo_fim DATE NOT NULL,
  total_pedidos INTEGER DEFAULT 0,
  valor_vendido DECIMAL(10, 2) DEFAULT 0,
  quantidade_unidades INTEGER DEFAULT 0,
  taxa_comissao DECIMAL(5, 2) DEFAULT 0 CHECK (taxa_comissao >= 0 AND taxa_comissao <= 100),
  valor_comissao DECIMAL(10, 2) GENERATED ALWAYS AS (valor_vendido * taxa_comissao / 100) STORED,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDENTE'
    CHECK (status IN ('PENDENTE', 'PAGO')),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_commissions_vendedor_id ON commissions(vendedor_id);
CREATE INDEX idx_commissions_periodo ON commissions(periodo_inicio, periodo_fim);
CREATE INDEX idx_commissions_status ON commissions(status);
```

---

## 3. VIEWS (Consultas úteis)

### 3.1 Resumo de Pedidos por Cliente

```sql
CREATE VIEW vw_customer_summary AS
SELECT 
  c.id,
  c.nome,
  COUNT(o.id) as total_pedidos,
  COUNT(CASE WHEN o.status = 'RETIRADO' THEN 1 END) as pedidos_finalizados,
  SUM(o.valor_total) as total_gasto,
  MAX(o.created_at) as ultimo_pedido,
  CURRENT_DATE - DATE(MAX(o.created_at)) as dias_desde_ultima_compra
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id
GROUP BY c.id, c.nome;
```

---

### 3.2 Performance de Vendedores (Mês Atual)

```sql
CREATE VIEW vw_seller_performance_current_month AS
SELECT 
  u.id,
  u.nome,
  COUNT(o.id) as total_pedidos,
  COUNT(CASE WHEN o.status = 'RETIRADO' THEN 1 END) as pedidos_finalizados,
  SUM(CASE WHEN o.status = 'RETIRADO' THEN o.valor_total ELSE 0 END) as valor_vendido,
  SUM(CASE WHEN o.status = 'RETIRADO' THEN (
    SELECT SUM(quantidade) FROM order_items WHERE order_id = o.id
  ) ELSE 0 END) as quantidade_unidades
FROM users u
LEFT JOIN orders o ON u.id = o.criado_por 
  AND DATE_TRUNC('month', o.created_at) = DATE_TRUNC('month', CURRENT_TIMESTAMP)
WHERE u.role = 'vendedor' AND u.ativo = true
GROUP BY u.id, u.nome;
```

---

### 3.3 Status de Estoque

```sql
CREATE VIEW vw_inventory_status AS
SELECT 
  p.id,
  p.nome,
  p.sku,
  i.quantidade as quantidade_atual,
  p.estoque_minimo,
  CASE 
    WHEN i.quantidade <= p.estoque_minimo THEN 'CRÍTICO'
    WHEN i.quantidade <= p.estoque_minimo * 1.5 THEN 'BAIXO'
    ELSE 'OK'
  END as status_estoque
FROM products p
LEFT JOIN inventory i ON p.id = i.product_id
WHERE p.ativo = true
ORDER BY status_estoque DESC, p.nome;
```

---

## 4. TRIGGERS E FUNÇÕES

### 4.1 Atualizar updated_at automaticamente

```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar em todas as tabelas com updated_at
CREATE TRIGGER trigger_users_updated_at 
  BEFORE UPDATE ON users
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_customers_updated_at 
  BEFORE UPDATE ON customers
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_products_updated_at 
  BEFORE UPDATE ON products
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_inventory_updated_at 
  BEFORE UPDATE ON inventory
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_orders_updated_at 
  BEFORE UPDATE ON orders
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_returns_updated_at 
  BEFORE UPDATE ON returns
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_commissions_updated_at 
  BEFORE UPDATE ON commissions
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();
```

---

### 4.2 Gerar número de pedido automático

```sql
CREATE SEQUENCE order_numero_seq START 1000;

CREATE OR REPLACE FUNCTION generate_order_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.numero_pedido IS NULL THEN
        NEW.numero_pedido := 'PED-' || LPAD(nextval('order_numero_seq')::text, 6, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_order_numero
  BEFORE INSERT ON orders
  FOR EACH ROW
  EXECUTE FUNCTION generate_order_number();
```

---

### 4.3 Validação de Estoque ao Marcar Atendido

```sql
-- Esta lógica é melhor implementada no backend (Spring)
-- Mas aqui está um exemplo de constraint:

ALTER TABLE orders 
ADD CONSTRAINT check_status_workflow 
CHECK (
  -- PENDENTE não tem data de atendimento
  (status = 'PENDENTE' AND data_atendimento IS NULL AND data_retirada IS NULL)
  OR
  -- ATENDIDO tem data de atendimento mas não de retirada
  (status = 'ATENDIDO' AND data_atendimento IS NOT NULL AND data_retirada IS NULL)
  OR
  -- RETIRADO tem ambas as datas
  (status = 'RETIRADO' AND data_atendimento IS NOT NULL AND data_retirada IS NOT NULL)
);
```

---

## 5. DADOS INICIAIS (Seeds)

### 5.1 Usuários padrão

```sql
INSERT INTO users (email, password_hash, nome, role, telefone) VALUES
  ('admin@distribuidor.com', '$2a$10$...', 'Admin Master', 'admin', '11999999999'),
  ('gerente@distribuidor.com', '$2a$10$...', 'Gerente Estoque', 'gerente_estoque', '11988888888'),
  ('vendedor1@distribuidor.com', '$2a$10$...', 'João Vendedor', 'vendedor', '11987654321'),
  ('vendedor2@distribuidor.com', '$2a$10$...', 'Maria Vendedor', 'vendedor', '11987654322');
```

### 5.2 Produtos padrão (exemplo)

```sql
INSERT INTO products (nome, sku, preco_base, unidade) VALUES
  ('Luva Cirúrgica Tamanho M', 'LUV-M-001', 0.50, 'par'),
  ('Seringa 10ml', 'SER-10-001', 1.20, 'unidade'),
  ('Máscara Descartável', 'MAS-001', 0.35, 'unidade'),
  ('Gaze Estéril 10x10', 'GAZ-001', 2.50, 'pacote'),
  ('Álcool 70% 500ml', 'ALC-70-001', 8.90, 'frasco');

-- Criar registros de estoque para cada produto
INSERT INTO inventory (product_id, quantidade) 
SELECT id, 1000 FROM products;
```

---

## 6. ÍNDICES PARA PERFORMANCE

```sql
-- Já criados nas tabelas acima, mas aqui está um resumo:

-- Pesquisas frequentes
CREATE INDEX idx_orders_status_created ON orders(status, created_at DESC);
CREATE INDEX idx_order_items_order_product ON order_items(order_id, product_id);
CREATE INDEX idx_inventory_movements_product_date ON inventory_movements(product_id, created_at DESC);

-- Relatórios e views
CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);
CREATE INDEX idx_orders_vendedor_date ON orders(criado_por, created_at DESC);

-- Foreign keys (já implícitos, mas explícito ajuda)
CREATE INDEX idx_orders_customer_fk ON orders(customer_id);
CREATE INDEX idx_orders_user_fk ON orders(criado_por);
```

---

## 7. BACKUP E RECOVERY

### Strategy
- Backup diário completo (Supabase automático)
- Retenção: 30 dias
- Point-in-time recovery disponível

### Teste de Restore (mensal)
```bash
pg_dump -U postgres saas_db > backup_$(date +%Y%m%d).sql
psql -U postgres < backup_YYYYMMDD.sql
```

---

## 8. SEGURANÇA DO BANCO

- ✓ Senhas com hash (bcrypt, não em plain text)
- ✓ Foreign keys ativas (integridade referencial)
- ✓ Constraints adequadas (NOT NULL, CHECK)
- ✓ Índices em colunas frequentemente filtradas
- ✓ Auditoria: todos INSERT/UPDATE/DELETE têm timestamp

---

## 9. PRÓXIMOS PASSOS

1. Executar scripts de criação de tabelas em ambiente de teste
2. Validar relacionamentos e constraints
3. Popular com dados de teste
4. Testar performance com 10k+ pedidos
5. Documentar access patterns para otimização

---

**Fim do documento de Schema**
