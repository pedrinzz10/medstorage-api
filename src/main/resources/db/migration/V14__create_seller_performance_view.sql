-- Performance do vendedor no mes corrente: so contabiliza pedidos RETIRADO
-- (regra de negocio em docs/specs/04-business-rules.md, item 5).
CREATE OR REPLACE VIEW vw_seller_performance_current_month AS
SELECT
    u.id                                         AS vendedor_id,
    u.nome                                       AS vendedor_nome,
    u.email                                      AS vendedor_email,
    COUNT(DISTINCT o.id)                         AS total_pedidos,
    COALESCE(SUM(o.valor_total), 0)              AS valor_vendido,
    COALESCE(SUM(oi_totals.total_unidades), 0)   AS quantidade_unidades
FROM users u
INNER JOIN orders o
       ON  o.criado_por = u.id
       AND o.status     = 'RETIRADO'
       AND EXTRACT(YEAR  FROM o.data_retirada) = EXTRACT(YEAR  FROM CURRENT_DATE)
       AND EXTRACT(MONTH FROM o.data_retirada) = EXTRACT(MONTH FROM CURRENT_DATE)
LEFT JOIN (
    SELECT order_id, SUM(quantidade) AS total_unidades
    FROM order_items
    GROUP BY order_id
) oi_totals ON oi_totals.order_id = o.id
WHERE u.role  = 'vendedor'
  AND u.ativo = true
GROUP BY u.id, u.nome, u.email;
