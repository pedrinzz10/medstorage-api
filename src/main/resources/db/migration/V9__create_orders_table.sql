CREATE SEQUENCE order_numero_seq START 1000;

CREATE TABLE orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  numero_pedido VARCHAR(50) UNIQUE,
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
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT check_status_workflow CHECK (
    (status = 'PENDENTE' AND data_atendimento IS NULL AND data_retirada IS NULL)
    OR (status = 'ATENDIDO' AND data_atendimento IS NOT NULL AND data_retirada IS NULL)
    OR (status = 'RETIRADO' AND data_atendimento IS NOT NULL AND data_retirada IS NOT NULL)
  )
);

-- A aplicacao gera numero_pedido em Java (OrderService.generateNumeroPedido,
-- usando a mesma sequence). Este trigger fica como rede de seguranca para
-- qualquer insert direto via SQL que esqueca de preencher a coluna.
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

CREATE TRIGGER trigger_orders_updated_at
  BEFORE UPDATE ON orders
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_orders_numero ON orders(numero_pedido);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_criado_por ON orders(criado_por);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
