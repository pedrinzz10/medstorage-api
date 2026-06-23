# 06. User Stories (referência)

Texto completo em Gherkin: `docs/inicial_docs/REQUISITOS_SAAS_DISTRIBUIDOR.md`, seção 3. Resumo por epic, com link para a regra/endpoint correspondente:

| US | Resumo | Specs relacionadas |
|---|---|---|
| US-001 | Vendedor cria pedido com itens, desconto, valor calculado | `03-api-contract.md` (POST /orders), `04-business-rules.md` (desconto máx. 50%) |
| US-002 | Vendedor lista/filtra/ordena pedidos | `03-api-contract.md` (GET /orders, filtros) |
| US-003 | Gerente marca pedido ATENDIDO → decrementa estoque + email | `04-business-rules.md` (regra 2, 4, 8), Sprint 4 |
| US-004 | Gerente marca pedido RETIRADO → finaliza | Sprint 5 |
| US-005 | Decremento de estoque ao marcar ATENDIDO (histórico de movimento) | `02-data-model.md` (inventory_movements) |
| US-006 | Incremento de estoque ao devolver | Sprint 6, `04-business-rules.md` regra 6 |
| US-007 | Histórico de cliente (pedidos, total gasto, última compra) | `02-data-model.md` (vw_customer_summary) |
| US-008 | Vendedor vê sua performance (vendas, comissão, ranking) | Sprint 7 |
| US-009 | Admin vê performance de todos os vendedores | Sprint 7 |
| US-010 | Registrar devolução com motivo, reembolso | Sprint 6 |
| US-011 | Email quando pedido fica ATENDIDO | `04-business-rules.md` regra 4 |

Critérios de aceitação detalhados de cada US devem ser revisados no documento original antes de implementar o respectivo sprint.
