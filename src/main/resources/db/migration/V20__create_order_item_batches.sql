-- Rateio de lotes consumidos por item de pedido (alocacao FEFO + rastreabilidade).
CREATE TABLE order_item_batches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
  batch_id UUID NOT NULL REFERENCES product_batches(id) ON DELETE RESTRICT,
  quantidade_consumida INTEGER NOT NULL CHECK (quantidade_consumida > 0),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_oib_order_item_id ON order_item_batches(order_item_id);
CREATE INDEX idx_oib_batch_id ON order_item_batches(batch_id);
