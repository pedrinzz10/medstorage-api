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

## PATCH /api/orders/{id}/status
**Papel exigido:** `gerente_estoque` ou `admin`.

**Request:** `{ "newStatus": "ATENDIDO" }`

> **Escopo desta sprint:** só a transição para `ATENDIDO` está implementada. Qualquer outro valor de `newStatus` (incluindo `RETIRADO`) retorna 400 `"Status transition to 'X' is not supported yet"` — `RETIRADO` é Sprint 5.

Ao marcar `ATENDIDO`, dentro de uma única transação:
1. Para cada item do pedido, verifica estoque suficiente (`inventory.quantidade >= item.quantidade`)
2. Decrementa `inventory.quantidade` e cria um `InventoryMovement` (`tipo=OUT`, `motivo="Pedido {numeroPedido}"`)
3. Atualiza `order.status = ATENDIDO` e `data_atendimento`
4. **Depois que a transação é confirmada** (não antes — ver `docs/decisoes-tecnicas` para o porquê), envia o email de notificação ao cliente

**200 OK** com `status: "ATENDIDO"` e `dataAtendimento` preenchida.
**400** — estoque insuficiente em algum item (nenhuma alteração é persistida — transação inteira é revertida), ou pedido não está em `PENDENTE` (workflow é unidirecional).
**404** — pedido ou inventário do produto não encontrado.
**403** — papel diferente de gerente/admin.

## Email de notificação
Falha no envio (ex.: token Mailtrap inválido/ausente) é capturada e logada como warning — **nunca bloqueia ou desfaz** a transição de status, conforme `docs/specs/04-business-rules.md` regra 4. Não há fila de retry automática nesta sprint (ver `docs/ferramentas/mailtrap.md`).
