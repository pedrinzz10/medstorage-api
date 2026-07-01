-- Material consignado: enviado ao cliente mas ainda de propriedade da
-- distribuidora ate ser usado em procedimento e faturado.
CREATE TABLE consignments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
  status VARCHAR(20) NOT NULL DEFAULT 'ATIVO' CHECK (status IN ('ATIVO','ENCERRADO')),
  observacoes VARCHAR(500),
  criado_por UUID REFERENCES users(id) ON DELETE SET NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE consignment_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  consignment_id UUID NOT NULL REFERENCES consignments(id) ON DELETE CASCADE,
  product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
  batch_id UUID REFERENCES product_batches(id) ON DELETE RESTRICT,
  quantidade_enviada INTEGER NOT NULL CHECK (quantidade_enviada > 0),
  quantidade_usada INTEGER NOT NULL DEFAULT 0 CHECK (quantidade_usada >= 0),
  quantidade_devolvida INTEGER NOT NULL DEFAULT 0 CHECK (quantidade_devolvida >= 0),
  preco_unitario NUMERIC(10,2) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CHECK (quantidade_usada + quantidade_devolvida <= quantidade_enviada)
);

CREATE INDEX idx_consignments_customer_id ON consignments(customer_id);
CREATE INDEX idx_consignment_items_consignment_id ON consignment_items(consignment_id);

CREATE TRIGGER trigger_consignments_updated_at
  BEFORE UPDATE ON consignments
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();
