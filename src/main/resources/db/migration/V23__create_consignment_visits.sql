-- Agenda de visitas para conferencia mensal de material consignado no cliente.
CREATE TABLE consignment_visits (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
  funcionario_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  data_agendada DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'AGENDADA' CHECK (status IN ('AGENDADA','REALIZADA','CANCELADA')),
  observacoes VARCHAR(500),
  criado_por UUID REFERENCES users(id) ON DELETE SET NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_visits_data_agendada ON consignment_visits(data_agendada);
CREATE INDEX idx_visits_customer_id ON consignment_visits(customer_id);

CREATE TRIGGER trigger_consignment_visits_updated_at
  BEFORE UPDATE ON consignment_visits
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();
