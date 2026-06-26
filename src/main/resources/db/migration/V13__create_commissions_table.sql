CREATE TABLE commissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendedor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  periodo_inicio DATE NOT NULL,
  periodo_fim DATE NOT NULL,
  total_pedidos INTEGER NOT NULL DEFAULT 0 CHECK (total_pedidos >= 0),
  valor_vendido DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (valor_vendido >= 0),
  quantidade_unidades INTEGER NOT NULL DEFAULT 0 CHECK (quantidade_unidades >= 0),
  taxa_comissao DECIMAL(5, 2) NOT NULL DEFAULT 0 CHECK (taxa_comissao >= 0 AND taxa_comissao <= 100),
  valor_comissao DECIMAL(10, 2) GENERATED ALWAYS AS (valor_vendido * taxa_comissao / 100) STORED,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'PAGO')),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trigger_commissions_updated_at
  BEFORE UPDATE ON commissions
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_commissions_vendedor_id ON commissions(vendedor_id);
CREATE INDEX idx_commissions_status ON commissions(status);
CREATE INDEX idx_commissions_periodo ON commissions(periodo_inicio, periodo_fim);
