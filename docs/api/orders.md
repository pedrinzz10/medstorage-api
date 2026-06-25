# API — Pedidos

## POST /api/orders
**Papel exigido:** `vendedor` ou `admin` (matriz de permissões em `docs/specs/01-overview.md`).

**Request:**
```json
{
  "customerId": "uuid",
  "items": [{ "productId": "uuid", "quantidade": 50 }],
  "descontoAplicado": 0,
  "tipoDesconto": null,
  "notas": null
}
```
- `precoUnitario` **não é enviado pelo cliente** — é resolvido automaticamente a partir de `product.precoBase` no momento da criação (evita manipulação de preço pelo frontend).
- `valorTotal` = soma dos subtotais − `descontoAplicado`.
- `numeroPedido` é gerado pelo banco (trigger `generate_order_number`, formato `PED-NNNNNN`).

**201 Created:** pedido com `status: "PENDENTE"`, `items` com `productNome`/`subtotal` resolvidos.
**400** — lista de itens vazia, desconto negativo, ou desconto > 50% do valor bruto.
**404** — `customerId` ou algum `productId` não existe.
**401/403** — sem token / papel diferente de vendedor/admin.

## GET /api/orders/{id}
**200 OK:** detalhe completo do pedido. **404** — não encontrado. Qualquer papel autenticado pode consultar.

## GET /api/orders
Lista pedidos com filtros opcionais, paginação e ordenação. Qualquer papel autenticado pode consultar.

**Query params (todos opcionais):**
| Param | Tipo | Descrição |
|---|---|---|
| `status` | `PENDENTE`/`ATENDIDO`/`RETIRADO` | filtra por status exato |
| `customerId` | UUID | filtra pedidos de um cliente |
| `criadoPor` | UUID | filtra pedidos criados por um usuário (vendedor) |
| `dataInicio` | ISO datetime (`2026-06-01T00:00:00`) | `createdAt >= dataInicio` |
| `dataFim` | ISO datetime | `createdAt <= dataFim` |
| `valorMin` | decimal | `valorTotal >= valorMin` |
| `valorMax` | decimal | `valorTotal <= valorMax` |
| `page`, `size`, `sort` | paginação Spring padrão | default `size=20` |

Todos os filtros são combinados com `AND`. **200 OK** com `Page<OrderResponse>` (`content`, `totalElements`, `totalPages`, etc.).

## PATCH /api/orders/{id}/status
**Papel exigido:** `gerente_estoque` ou `admin`.

**Request:** `{ "newStatus": "ATENDIDO" }` ou `{ "newStatus": "RETIRADO" }`

Workflow é estritamente sequencial: `PENDENTE → ATENDIDO → RETIRADO`. Qualquer outro valor de `newStatus`, ou uma transição fora dessa ordem (ex.: `PENDENTE → RETIRADO`), retorna 400.

**ATENDIDO** — dentro de uma única transação:
1. Para cada item do pedido, verifica estoque suficiente (`inventory.quantidade >= item.quantidade`)
2. Decrementa `inventory.quantidade` e cria um `InventoryMovement` (`tipo=OUT`, `motivo="Pedido {numeroPedido}"`)
3. Atualiza `order.status = ATENDIDO` e `data_atendimento`
4. **Depois que a transação é confirmada** (não antes — ver `docs/decisoes-tecnicas` para o porquê), envia o email de notificação ao cliente

**RETIRADO** — exige que o pedido já esteja em `ATENDIDO`; apenas atualiza `order.status = RETIRADO` e `data_retirada` (não toca em estoque — a baixa já ocorreu ao marcar `ATENDIDO`).

**200 OK** com o novo `status` e a respectiva data (`dataAtendimento`/`dataRetirada`) preenchida.
**400** — estoque insuficiente em algum item ao marcar `ATENDIDO` (transação inteira é revertida), ou pedido fora da ordem esperada do workflow.
**404** — pedido ou inventário do produto não encontrado.
**403** — papel diferente de gerente/admin.

## Email de notificação
Falha no envio (ex.: credenciais SMTP inválidas/ausentes) é capturada e logada como warning — **nunca bloqueia ou desfaz** a transição de status, conforme `docs/specs/04-business-rules.md` regra 4. Não há fila de retry automática nesta sprint (ver `docs/ferramentas/smtp-mailgun.md`).
