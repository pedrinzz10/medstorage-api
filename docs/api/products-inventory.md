# API — Produtos e Estoque

Todos os endpoints exigem `Authorization: Bearer <token>` (qualquer papel autenticado — não há restrição por role nestas leituras, já que vendedores também precisam consultar o catálogo para criar pedidos).

## GET /api/products
Lista produtos **ativos** (`ativo = true`), paginado.

**Query params:** `page` (default 0), `size` (default 20) — paginação nativa do Spring Data.

**200 OK:**
```json
{
  "content": [
    { "id": "uuid", "nome": "Luva Cirurgica Tamanho M", "descricao": null, "sku": "LUV-M-001", "precoBase": 0.50, "unidade": "par", "estoqueMinimo": 100, "ativo": true }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```
> Nota: o formato de paginação usa os campos nativos do Spring Data (`number`, `size`) em vez de `currentPage`/`pageSize` mencionados no rascunho original da documentação — mantém o endpoint idiomático ao framework em vez de reimplementar um wrapper customizado.

## GET /api/products/{id}
**200 OK:** mesmo formato de um item de `content` acima. **404** — produto não existe.

## GET /api/inventory/status
Lista o estoque de todos os produtos ativos, com status calculado: `CRITICO` (quantidade ≤ estoque mínimo), `BAIXO` (≤ 1.5x o mínimo), `OK` (acima disso). Ordenado por severidade (`CRITICO` → `BAIXO` → `OK`), depois por nome.

**200 OK:**
```json
[
  { "id": "uuid", "nome": "Luva Cirurgica Tamanho M", "sku": "LUV-M-001", "quantidadeAtual": 1000, "estoqueMinimo": 100, "statusEstoque": "OK" }
]
```
> Nota: a ordenação por severidade (mais crítico primeiro) é uma correção deliberada em relação ao documento original, que ordenava por `status_estoque DESC` em texto puro — isso não produz uma ordem de severidade coerente (ordem alfabética não é ordem de urgência).

## GET /api/inventory/{productId}
**200 OK:** mesmo formato de um item da lista de status acima. **404** — produto sem registro de estoque.

## Decisão de escopo
`POST /api/inventory/movements` (mencionado no contrato original) **não foi implementado nesta sprint** — não há ainda nenhuma operação que gere um movimento de estoque (isso só passa a existir na Sprint 4, ao marcar pedido como ATENDIDO). Implementar esse endpoint agora seria prematuro, sem nenhum produtor real de dados.
