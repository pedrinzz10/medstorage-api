CREATE TABLE products (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nome VARCHAR(255) NOT NULL,
  descricao TEXT,
  sku VARCHAR(100) UNIQUE NOT NULL,
  preco_base DECIMAL(10, 2) NOT NULL CHECK (preco_base >= 0),
  unidade VARCHAR(50) DEFAULT 'unidade',
  estoque_minimo INTEGER DEFAULT 0 CHECK (estoque_minimo >= 0),
  ativo BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_nome ON products(nome);
CREATE INDEX idx_products_ativo ON products(ativo);

CREATE TRIGGER trigger_products_updated_at
  BEFORE UPDATE ON products
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();
