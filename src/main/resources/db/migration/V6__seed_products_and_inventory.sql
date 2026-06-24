INSERT INTO products (nome, sku, preco_base, unidade, estoque_minimo) VALUES
  ('Luva Cirurgica Tamanho M', 'LUV-M-001', 0.50, 'par', 100),
  ('Seringa 10ml', 'SER-10-001', 1.20, 'unidade', 200),
  ('Mascara Descartavel', 'MAS-001', 0.35, 'unidade', 300),
  ('Gaze Esteril 10x10', 'GAZ-001', 2.50, 'pacote', 50),
  ('Alcool 70% 500ml', 'ALC-70-001', 8.90, 'frasco', 80);

INSERT INTO inventory (product_id, quantidade)
SELECT id, 1000 FROM products;
