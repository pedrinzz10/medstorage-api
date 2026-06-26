# API — Clientes

Todos os endpoints exigem `Authorization: Bearer <token>` (qualquer papel autenticado — não há restrição por role no CRUD de clientes, diferente de `/api/auth/register`).

## POST /api/customers
**Request:**
```json
{
  "nome": "Hospital Central",
  "email": "compras@hospitalcentral.com",
  "telefone": "11999999999",
  "cnpj": "12345678901234",
  "endereco": "Rua X, 100",
  "contatoPrincipal": "Maria",
  "dadosAdicionais": { "observacao": "cliente VIP" }
}
```
**201 Created:** mesmo formato com `id` preenchido. **400** — falta `nome` ou `email` (validados na API mesmo que a coluna `email` no banco seja opcional — ver nota abaixo).

## GET /api/customers
Lista paginada (`page`, `size`, default `size=20`), mesmo formato de paginação nativo do Spring Data usado em `/api/products`.

## GET /api/customers/{id}
**200 OK:** detalhe do cliente. **404** — não encontrado.

> **Fora de escopo nesta sprint:** o "resumo de compras" (total de pedidos, valor gasto, última compra — `vw_customer_summary` em `docs/specs/02-data-model.md`) e o endpoint `GET /api/customers/{id}/orders` dependem da entidade `Order`, que só existe a partir da Sprint 4. Implementá-los agora exigiria dados fictícios. Serão adicionados quando pedidos existirem.

## PUT /api/customers/{id}
Substitui todos os campos editáveis (mesmo payload do POST). **200 OK** — cliente atualizado. **404** — não encontrado. **400** — validação falhou.

## Nota sobre validação de `email`
A coluna `email` no banco é opcional (`docs/specs/02-data-model.md`), mas a API exige `@NotBlank @Email` no request — decisão deliberada: a regra de negócio (US e testes do guia original) exige email para notificações de pedido, então validamos na borda da API em vez de relaxar a constraint do banco. Mantém a tabela flexível para fluxos futuros (ex.: importação de dados legados) sem abrir brecha na API atual.
