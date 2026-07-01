-- Registro de faturamento quando o hospital reporta uso do material consignado.
CREATE TABLE consignment_usages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  consignment_item_id UUID NOT NULL REFERENCES consignment_items(id) ON DELETE RESTRICT,
  quantidade INTEGER NOT NULL CHECK (quantidade > 0),
  valor_faturado NUMERIC(10,2) NOT NULL,
  data_uso DATE NOT NULL,
  criado_por UUID REFERENCES users(id) ON DELETE SET NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consignment_usages_item_id ON consignment_usages(consignment_item_id);
