-- Contagem fisica mensal do material consignado no cliente: confere
-- quantidade, lote e validade contra o que o sistema espera, revelando
-- divergencia (material usado e nao reportado, ou sobra inesperada).
CREATE TABLE consignment_counts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
  visit_id UUID REFERENCES consignment_visits(id) ON DELETE SET NULL,
  funcionario_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  data_contagem DATE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE consignment_count_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  consignment_count_id UUID NOT NULL REFERENCES consignment_counts(id) ON DELETE CASCADE,
  consignment_item_id UUID NOT NULL REFERENCES consignment_items(id) ON DELETE RESTRICT,
  quantidade_contada INTEGER NOT NULL CHECK (quantidade_contada >= 0),
  lote_conferido VARCHAR(60),
  validade_conferida DATE,
  divergencia INTEGER NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consignment_counts_customer_id ON consignment_counts(customer_id);
CREATE INDEX idx_count_items_count_id ON consignment_count_items(consignment_count_id);
