CREATE VIEW vw_customer_summary AS
SELECT
    c.id                                                                       AS customer_id,
    COUNT(o.id)                                                                AS total_pedidos,
    COALESCE(SUM(CASE WHEN o.status = 'RETIRADO' THEN o.valor_total ELSE 0 END), 0) AS valor_total_gasto,
    MAX(CASE WHEN o.status = 'RETIRADO' THEN o.created_at END)                AS ultima_compra
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
GROUP BY c.id;

CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);
