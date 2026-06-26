CREATE SEQUENCE return_numero_seq START 1000;

CREATE TABLE returns (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  numero_retorno VARCHAR(50) UNIQUE,
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
  processado_por UUID REFERENCES users(id) ON DELETE SET NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDENTE'
    CHECK (status IN ('PENDENTE', 'PROCESSADO', 'REJEITADO')),
  motivo TEXT,
  data_solicitacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  data_processamento TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- A aplicacao gera numero_retorno em Java (ReturnService.generateNumeroRetorno,
-- usando a mesma sequence). Este trigger fica como rede de seguranca para
-- qualquer insert direto via SQL que esqueca de preencher a coluna.
CREATE OR REPLACE FUNCTION generate_return_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.numero_retorno IS NULL THEN
        NEW.numero_retorno := 'DEV-' || LPAD(nextval('return_numero_seq')::text, 6, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_return_numero
  BEFORE INSERT ON returns
  FOR EACH ROW
  EXECUTE FUNCTION generate_return_number();

CREATE TRIGGER trigger_returns_updated_at
  BEFORE UPDATE ON returns
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_returns_numero ON returns(numero_retorno);
CREATE INDEX idx_returns_order_id ON returns(order_id);
CREATE INDEX idx_returns_status ON returns(status);
CREATE INDEX idx_returns_created_at ON returns(created_at);
