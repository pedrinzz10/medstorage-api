-- View documentada em docs/specs/02-data-model.md. Mantida para consulta
-- manual/relatorios; o endpoint GET /api/inventory/status calcula o status
-- em Java (InventoryService) para ficar testavel com testes unitarios,
-- nao consulta esta view diretamente.
CREATE VIEW vw_inventory_status AS
SELECT
  p.id,
  p.nome,
  p.sku,
  i.quantidade AS quantidade_atual,
  p.estoque_minimo,
  CASE
    WHEN i.quantidade <= p.estoque_minimo THEN 'CRITICO'
    WHEN i.quantidade <= p.estoque_minimo * 1.5 THEN 'BAIXO'
    ELSE 'OK'
  END AS status_estoque
FROM products p
LEFT JOIN inventory i ON p.id = i.product_id
WHERE p.ativo = true;
