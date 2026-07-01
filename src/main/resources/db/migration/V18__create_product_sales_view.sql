-- Vendas por produto (apenas pedidos FINALIZADO) para a analise de curva ABC
-- (docs/decisoes-tecnicas — classificacao de produtos por relevancia de venda).
CREATE VIEW vw_product_sales AS
SELECT
    p.id                                    AS product_id,
    p.nome                                  AS nome,
    p.sku                                   AS sku,
    COALESCE(SUM(oi.subtotal), 0)           AS valor_vendido,
    COALESCE(SUM(oi.quantidade), 0)         AS quantidade_vendida
FROM products p
LEFT JOIN order_items oi ON oi.product_id = p.id
LEFT JOIN orders o       ON o.id = oi.order_id AND o.status = 'FINALIZADO'
GROUP BY p.id, p.nome, p.sku;
