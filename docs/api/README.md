# API

Documentação viva dos endpoints conforme implementados (exemplos reais de request/response testados, não apenas o contrato planejado).

## Implementado
- [auth.md](auth.md) — login, register, validate, refresh, logout (Sprint 1)

## Referências de planejamento
- Contrato completo planejado: `docs/specs/03-api-contract.md`
- Regras de negócio aplicadas nos endpoints: `docs/specs/04-business-rules.md`

## Convenção
Um arquivo por grupo de recurso, criado quando o grupo é implementado:
- `auth.md`, `orders.md`, `customers.md`, `products-inventory.md`, `returns.md`, `performance.md`

Cada arquivo deve conter, por endpoint: método+path, request de exemplo, response de exemplo (sucesso e erro), e regras de autorização aplicadas.
