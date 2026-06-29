ALTER TABLE orders
    ADD COLUMN retirado_por UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_orders_retirado_por ON orders(retirado_por);
