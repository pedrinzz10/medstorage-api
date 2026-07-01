-- Controle de lote/validade por produto (rastreabilidade e alocação FEFO).
CREATE TABLE product_batches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
  lote VARCHAR(60) NOT NULL,
  validade DATE NOT NULL,
  quantidade INTEGER NOT NULL DEFAULT 0 CHECK (quantidade >= 0),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (product_id, lote)
);

CREATE INDEX idx_batches_product_id ON product_batches(product_id);
CREATE INDEX idx_batches_validade ON product_batches(validade);

CREATE TRIGGER trigger_product_batches_updated_at
  BEFORE UPDATE ON product_batches
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();
