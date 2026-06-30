-- =====================================================================
-- V17: Expande fluxo de status (PENDENTE/ATENDIDO/RETIRADO → 5 estados)
-- e adiciona coluna de reserva de estoque em inventory.
-- =====================================================================

-- 1. Remover constraints antigas de status e workflow
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS check_status_workflow;

-- 2. Migrar dados existentes para novos status
UPDATE orders SET status = 'FINALIZADO' WHERE status = 'RETIRADO';
UPDATE orders SET status = 'PRONTO'     WHERE status = 'ATENDIDO';
UPDATE orders SET status = 'CRIADO'     WHERE status = 'PENDENTE';

-- 3. Renomear colunas de timestamp e responsavel
ALTER TABLE orders RENAME COLUMN data_atendimento TO data_pronto;
ALTER TABLE orders RENAME COLUMN data_retirada    TO data_finalizado;
ALTER TABLE orders RENAME COLUMN retirado_por     TO finalizado_por;

-- 4. Adicionar novos timestamps das etapas intermediarias
ALTER TABLE orders ADD COLUMN data_confirmado TIMESTAMP;
ALTER TABLE orders ADD COLUMN data_separado   TIMESTAMP;

-- 5. Popular timestamps migrados:
--    pedidos PRONTO/FINALIZADO já tinham data_atendimento (agora data_pronto),
--    por isso preenchemos as etapas anteriores com o mesmo valor.
UPDATE orders
SET data_confirmado = data_pronto,
    data_separado   = data_pronto
WHERE status IN ('PRONTO', 'FINALIZADO')
  AND data_pronto IS NOT NULL;

-- 6. Atualizar default de status
ALTER TABLE orders ALTER COLUMN status SET DEFAULT 'CRIADO';

-- 7. Nova constraint de valores validos
ALTER TABLE orders ADD CONSTRAINT orders_status_check
    CHECK (status IN ('CRIADO','CONFIRMADO','SEPARADO','PRONTO','FINALIZADO','CANCELADO'));

-- 8. Nova constraint de integridade do workflow de timestamps
ALTER TABLE orders ADD CONSTRAINT check_status_workflow CHECK (
    (status = 'CRIADO'
        AND data_confirmado IS NULL
        AND data_separado   IS NULL
        AND data_pronto     IS NULL
        AND data_finalizado IS NULL)
    OR (status = 'CONFIRMADO'
        AND data_confirmado IS NOT NULL
        AND data_separado   IS NULL
        AND data_pronto     IS NULL
        AND data_finalizado IS NULL)
    OR (status = 'SEPARADO'
        AND data_confirmado IS NOT NULL
        AND data_separado   IS NOT NULL
        AND data_pronto     IS NULL
        AND data_finalizado IS NULL)
    OR (status = 'PRONTO'
        AND data_confirmado IS NOT NULL
        AND data_separado   IS NOT NULL
        AND data_pronto     IS NOT NULL
        AND data_finalizado IS NULL)
    OR (status = 'FINALIZADO'
        AND data_confirmado IS NOT NULL
        AND data_separado   IS NOT NULL
        AND data_pronto     IS NOT NULL
        AND data_finalizado IS NOT NULL)
    OR (status = 'CANCELADO')
);

-- 9. Adicionar coluna de reserva de estoque
ALTER TABLE inventory
    ADD COLUMN quantidade_reservada INT NOT NULL DEFAULT 0
        CHECK (quantidade_reservada >= 0);

ALTER TABLE inventory
    ADD CONSTRAINT check_inventory_reserva
        CHECK (quantidade >= quantidade_reservada);

-- 10. Atualizar view de performance do vendedor (RETIRADO → FINALIZADO)
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
       AND o.status     = 'FINALIZADO'
       AND EXTRACT(YEAR  FROM o.data_finalizado) = EXTRACT(YEAR  FROM CURRENT_DATE)
       AND EXTRACT(MONTH FROM o.data_finalizado) = EXTRACT(MONTH FROM CURRENT_DATE)
LEFT JOIN (
    SELECT order_id, SUM(quantidade) AS total_unidades
    FROM order_items
    GROUP BY order_id
) oi_totals ON oi_totals.order_id = o.id
WHERE u.role  = 'vendedor'
  AND u.ativo = true
GROUP BY u.id, u.nome, u.email;

-- 11. Atualizar view de resumo de clientes (RETIRADO → FINALIZADO)
DROP VIEW IF EXISTS vw_customer_summary;

CREATE VIEW vw_customer_summary AS
SELECT
    c.id                                                                           AS customer_id,
    COUNT(o.id)                                                                    AS total_pedidos,
    COALESCE(SUM(CASE WHEN o.status = 'FINALIZADO' THEN o.valor_total ELSE 0 END), 0) AS valor_total_gasto,
    MAX(CASE WHEN o.status = 'FINALIZADO' THEN o.created_at END)                  AS ultima_compra
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
GROUP BY c.id;
