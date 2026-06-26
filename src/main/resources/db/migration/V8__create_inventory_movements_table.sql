CREATE TABLE inventory_movements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
  tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('IN', 'OUT')),
  quantidade INTEGER NOT NULL CHECK (quantidade > 0),
  motivo VARCHAR(255) NOT NULL,
  referencia_id UUID,
  referencia_tipo VARCHAR(50),
  criado_por UUID REFERENCES users(id) ON DELETE SET NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movements_product_id ON inventory_movements(product_id);
CREATE INDEX idx_movements_tipo ON inventory_movements(tipo);
CREATE INDEX idx_movements_created_at ON inventory_movements(created_at);
