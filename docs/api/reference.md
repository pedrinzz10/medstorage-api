# Referência Completa da API

Documentação consolidada de todos os endpoints implementados, com exemplos de request/response, campos obrigatórios e regras de autorização.

**Base URL:** `http://localhost:8080`  
**Content-Type:** `application/json` em todas as requisições com body  
**Autenticação:** cookie `jwt` (automático no navegador) ou header `Authorization: Bearer <token>`

---

## Sumário

- [Autenticação](#autenticação)
- [Clientes](#clientes)
- [Produtos](#produtos)
- [Estoque](#estoque)
- [Movimentações de estoque](#movimentações-de-estoque)
- [Pedidos](#pedidos)
- [Devoluções](#devoluções)
- [Comissões](#comissões)
- [Performance de vendedores](#performance-de-vendedores)
- [Usuários](#usuários)
- [Formato de erro](#formato-de-erro)
- [Paginação](#paginação)

---

## Autenticação

### POST /api/auth/login

Autentica o usuário e define o cookie JWT. Limitado a **5 tentativas por minuto por IP**.

**Auth:** Pública

**Request:**
```json
{
  "email": "admin@distribuidor.com",
  "password": "Admin123!"
}
```

**Response 200:**
```
Set-Cookie: jwt=eyJ...; HttpOnly; SameSite=Strict; Path=/

{
  "id": "a1b2c3d4-...",
  "email": "admin@distribuidor.com",
  "nome": "Administrador",
  "role": "admin"
}
```

**Erros:**
- `401` — credenciais inválidas
- `429` — limite de tentativas atingido

---

### POST /api/auth/register

Cria um novo usuário no sistema.

**Auth:** ADMIN

**Request:**
```json
{
  "email": "novo@distribuidor.com",
  "password": "Senha@123",
  "nome": "Nome Completo",
  "role": "vendedor",
  "telefone": "11999990000"
}
```

**Campos `role` aceitos:** `admin`, `gerente_estoque`, `vendedor`

**Response 201:**
```json
{
  "id": "uuid",
  "email": "novo@distribuidor.com",
  "nome": "Nome Completo",
  "role": "vendedor"
}
```

**Erros:**
- `400` — e-mail já cadastrado, role inválida ou campos obrigatórios ausentes
- `403` — não é ADMIN

---

### POST /api/auth/refresh

Renova o token e atualiza o cookie. Reflete o papel e status atuais do usuário no banco.

**Auth:** Autenticado (cookie ou `Authorization: Bearer`)

**Request:** sem body

**Response 200:**
```
Set-Cookie: jwt=eyJ...; HttpOnly; SameSite=Strict; Path=/

{
  "id": "uuid",
  "email": "...",
  "nome": "...",
  "role": "..."
}
```

**Erros:**
- `401` — token ausente, inválido ou usuário inativo

---

### POST /api/auth/logout

Limpa o cookie JWT no navegador.

**Auth:** Autenticado

**Request:** sem body

**Response 200:** sem body (cookie `jwt` é sobrescrito com `maxAge=0`)

---

### GET /api/auth/validate

Verifica se o token é válido e retorna dados do usuário autenticado.

**Auth:** Autenticado

**Response 200:**
```json
{
  "valid": true,
  "email": "admin@distribuidor.com",
  "role": "admin"
}
```

**Erros:**
- `401` — token ausente ou inválido

---

## Clientes

### POST /api/customers

Cadastra um novo cliente.

**Auth:** Autenticado

**Request:**
```json
{
  "nome": "Hospital São Luís",
  "email": "compras@saoluis.com.br",
  "telefone": "11333340000",
  "cnpj": "12.345.678/0001-99",
  "endereco": "Rua das Flores, 100 — São Paulo/SP",
  "contatoPrincipal": "Maria Souza",
  "dadosAdicionais": {
    "tipoContrato": "anual",
    "limiteCredito": 50000
  }
}
```

O campo `dadosAdicionais` é JSONB — aceita qualquer estrutura JSON livre sem schema fixo.

**Response 201:**
```json
{
  "id": "uuid",
  "nome": "Hospital São Luís",
  "email": "compras@saoluis.com.br",
  "telefone": "11333340000",
  "cnpj": "12.345.678/0001-99",
  "endereco": "Rua das Flores, 100 — São Paulo/SP",
  "contatoPrincipal": "Maria Souza",
  "dadosAdicionais": { "tipoContrato": "anual", "limiteCredito": 50000 }
}
```

---

### GET /api/customers

Lista clientes com paginação.

**Auth:** Autenticado

**Query params:**
- `page` (default: 0), `size` (default: 20), `sort` (ex.: `nome,asc`)

**Response 200:** `Page<CustomerResponse>` — ver [Paginação](#paginação)

---

### GET /api/customers/{id}

Detalhe do cliente com resumo de compras.

**Auth:** Autenticado

**Response 200:**
```json
{
  "id": "uuid",
  "nome": "Hospital São Luís",
  "email": "compras@saoluis.com.br",
  "telefone": "11333340000",
  "cnpj": "12.345.678/0001-99",
  "endereco": "...",
  "contatoPrincipal": "Maria Souza",
  "dadosAdicionais": {},
  "totalPedidos": 12,
  "valorTotalGasto": 48250.00,
  "ultimaCompra": "2025-06-28T14:30:00"
}
```

`totalPedidos` e `valorTotalGasto` consideram apenas pedidos com status `RETIRADO`.

**Erros:**
- `404` — cliente não encontrado

---

### PUT /api/customers/{id}

Atualiza dados cadastrais do cliente.

**Auth:** Autenticado

**Request:** mesmo formato do `POST /api/customers`

**Response 200:** `CustomerResponse`

---

### GET /api/customers/{id}/orders

Histórico de pedidos do cliente.

**Auth:** Autenticado

**Query params:** `page`, `size`

**Response 200:** `Page<OrderResponse>`

---

## Produtos

### GET /api/products

Lista produtos ativos com paginação.

**Auth:** Autenticado

**Query params:** `page` (default: 0), `size` (default: 20)

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "nome": "Luva Cirúrgica M",
      "descricao": "Luva estéril para procedimentos",
      "sku": "LUV-M-001",
      "precoBase": 0.50,
      "unidade": "par",
      "estoqueMinimo": 100,
      "ativo": true
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

### GET /api/products/{id}

Detalhe de um produto (retorna mesmo que esteja inativo).

**Auth:** Autenticado

**Response 200:** `ProductResponse`

**Erros:**
- `404` — produto não encontrado

---

### POST /api/products

Cria produto e inicializa entrada em `inventory` com `quantidade=0` na mesma transação.

**Auth:** ADMIN

**Request:**
```json
{
  "nome": "Máscara N95",
  "descricao": "Proteção respiratória nível N95",
  "sku": "MSK-N95-001",
  "precoBase": 12.90,
  "unidade": "unidade",
  "estoqueMinimo": 50,
  "ativo": true
}
```

**Campos obrigatórios:** `nome`, `sku`, `precoBase`

**Response 201:** `ProductResponse`

**Erros:**
- `400` — SKU duplicado, preço negativo ou campo obrigatório ausente
- `403` — não é ADMIN

---

### PUT /api/products/{id}

Atualiza dados do produto.

**Auth:** ADMIN

**Request:** mesmo formato do `POST /api/products`

**Response 200:** `ProductResponse`

---

### DELETE /api/products/{id}

Desativa o produto (soft delete — seta `ativo=false`). Não remove do banco.

**Auth:** ADMIN

**Response 204:** sem body

---

## Estoque

### GET /api/inventory/status

Lista todos os produtos com quantidade atual e classificação de criticidade, ordenados do mais crítico ao mais saudável.

**Auth:** Autenticado

**Response 200:**
```json
[
  {
    "id": "uuid",
    "nome": "Máscara Cirúrgica",
    "sku": "MSK-CRG-50",
    "quantidadeAtual": 8,
    "estoqueMinimo": 50,
    "statusEstoque": "CRITICO"
  },
  {
    "id": "uuid",
    "nome": "Luva Nitrila P",
    "sku": "LVN-P-100",
    "quantidadeAtual": 38,
    "estoqueMinimo": 30,
    "statusEstoque": "BAIXO"
  },
  {
    "id": "uuid",
    "nome": "Seringa 10ml",
    "sku": "SRG-10ML-001",
    "quantidadeAtual": 340,
    "estoqueMinimo": 50,
    "statusEstoque": "OK"
  }
]
```

**Classificação:**

| Status | Condição |
|---|---|
| `CRITICO` | `quantidadeAtual <= estoqueMinimo` |
| `BAIXO` | `quantidadeAtual <= estoqueMinimo × 1.5` |
| `OK` | Acima de 1.5× o mínimo |

---

### GET /api/inventory/{productId}

Status de estoque de um produto específico.

**Auth:** Autenticado

**Response 200:** `InventoryStatusResponse` (mesmo formato do item acima)

**Erros:**
- `404` — produto não encontrado

---

## Movimentações de estoque

### GET /api/inventory/movements

Histórico paginado de movimentações de entrada e saída.

**Auth:** Autenticado

**Query params:**
- `productId` — filtra por produto (UUID, opcional)
- `page`, `size`, `sort` (ex.: `createdAt,desc`)

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "productId": "uuid",
      "productNome": "Seringa 10ml",
      "tipo": "OUT",
      "quantidade": 50,
      "motivo": "Atendimento de pedido",
      "referenciaId": "uuid-do-pedido",
      "referenciaTipo": "ORDER",
      "criadoPorEmail": "gerente@distribuidor.com",
      "createdAt": "2025-06-30T10:15:00"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

**Tipos de movimentação:**
- `OUT` — saída por atendimento de pedido
- `IN` — entrada por processamento de devolução

---

## Pedidos

### POST /api/orders

Cria um pedido em status `PENDENTE`. O preço unitário de cada item é congelado no momento da criação.

**Auth:** VENDEDOR, ADMIN

**Request:**
```json
{
  "customerId": "uuid-do-cliente",
  "items": [
    { "productId": "uuid", "quantidade": 50 },
    { "productId": "uuid", "quantidade": 10 }
  ],
  "descontoAplicado": 5.00,
  "tipoDesconto": "PERCENTUAL",
  "notas": "Entrega urgente — UTI do Hospital Regional"
}
```

**Campos obrigatórios:** `customerId`, `items` (ao menos 1 item com `productId` e `quantidade > 0`)

**Response 201:**
```json
{
  "id": "uuid",
  "numeroPedido": "PED-001042",
  "customerId": "uuid",
  "customerNome": "Hospital São Luís",
  "criadoPor": "uuid-do-vendedor",
  "status": "PENDENTE",
  "valorTotal": 285.00,
  "descontoAplicado": 5.00,
  "tipoDesconto": "PERCENTUAL",
  "notas": "Entrega urgente — UTI do Hospital Regional",
  "dataAtendimento": null,
  "dataRetirada": null,
  "items": [
    {
      "id": "uuid",
      "productId": "uuid",
      "productNome": "Seringa 10ml",
      "quantidade": 50,
      "precoUnitario": 2.50,
      "subtotal": 125.00
    }
  ]
}
```

**Erros:**
- `400` — produto inativo, quantidade inválida, cliente inexistente
- `403` — usuário sem role VENDEDOR ou ADMIN

---

### GET /api/orders

Lista pedidos com filtros opcionais.

**Auth:** Autenticado

**Query params:**

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `status` | `PENDENTE` \| `ATENDIDO` \| `RETIRADO` | Filtra por status |
| `customerId` | UUID | Filtra pelo cliente |
| `criadoPor` | UUID | Filtra pelo vendedor |
| `dataInicio` | ISO-8601 | Ex.: `2025-01-01T00:00:00` |
| `dataFim` | ISO-8601 | Ex.: `2025-12-31T23:59:59` |
| `valorMin` | decimal | Valor total mínimo |
| `valorMax` | decimal | Valor total máximo |
| `page` | int | Default: 0 |
| `size` | int | Default: 20 |

**Response 200:** `Page<OrderResponse>`

---

### GET /api/orders/{id}

Detalhe do pedido com todos os itens.

**Auth:** Autenticado

**Response 200:** `OrderResponse` (ver exemplo no POST acima)

**Erros:**
- `404` — pedido não encontrado

---

### PUT /api/orders/{id}

Atualiza itens, desconto e notas. Somente para pedidos com status `PENDENTE`.

**Auth:** VENDEDOR, ADMIN

**Request:** mesmo formato do `POST /api/orders`

**Response 200:** `OrderResponse` atualizado

**Erros:**
- `400` — pedido não está `PENDENTE`
- `404` — pedido não encontrado

---

### DELETE /api/orders/{id}

Exclui o pedido. Somente para pedidos com status `PENDENTE`.

**Auth:** VENDEDOR, ADMIN

**Response 204:** sem body

**Erros:**
- `400` — pedido não está `PENDENTE`
- `404` — pedido não encontrado

---

### PATCH /api/orders/{id}/status

Avança o status do pedido. Transições válidas: `PENDENTE → ATENDIDO` e `ATENDIDO → RETIRADO`.

**Auth:** GERENTE_ESTOQUE, ADMIN

**Request:**
```json
{ "newStatus": "ATENDIDO" }
```

**Ao marcar como `ATENDIDO`:**
- Valida estoque disponível para todos os itens
- Decrementa estoque de cada produto
- Registra movimentações `OUT` em `inventory_movements`
- Envia e-mail ao cliente e à equipe (assíncrono, pós-commit)

**Ao marcar como `RETIRADO`:**
- Registra `dataRetirada` e `retiradoPor`
- Não altera estoque

**Response 200:** `OrderResponse` com status atualizado

**Erros:**
- `400` — estoque insuficiente ou transição de status inválida
- `403` — não é GERENTE_ESTOQUE nem ADMIN
- `404` — pedido não encontrado

---

## Devoluções

### POST /api/returns

Abre uma devolução. Somente para pedidos com status `RETIRADO`.

**Auth:** GERENTE_ESTOQUE, ADMIN

**Request:**
```json
{
  "orderId": "uuid-do-pedido",
  "items": [
    { "productId": "uuid", "quantidade": 2 }
  ],
  "motivo": "Produto com defeito — embalagem violada"
}
```

A quantidade de cada item não pode exceder a quantidade original no pedido.

**Response 201:**
```json
{
  "id": "uuid",
  "numeroRetorno": "DEV-001008",
  "orderId": "uuid",
  "numeroPedido": "PED-001042",
  "processadoPor": null,
  "status": "PENDENTE",
  "motivo": "Produto com defeito — embalagem violada",
  "dataSolicitacao": "2025-06-30T14:00:00",
  "dataProcessamento": null,
  "items": [
    {
      "id": "uuid",
      "productId": "uuid",
      "productNome": "Seringa 10ml",
      "quantidade": 2,
      "precoUnitario": 2.50,
      "subtotal": 5.00
    }
  ]
}
```

**Erros:**
- `400` — pedido não está `RETIRADO`, quantidade excede o original ou devolução já existe para o item
- `404` — pedido ou produto não encontrado

---

### GET /api/returns

Lista devoluções com paginação.

**Auth:** Autenticado

**Query params:** `page`, `size`

**Response 200:** `Page<ReturnResponse>`

---

### GET /api/returns/{id}

Detalhe da devolução.

**Auth:** Autenticado

**Response 200:** `ReturnResponse`

**Erros:**
- `404` — devolução não encontrada

---

### PATCH /api/returns/{id}/process

Processa a devolução: estoque é revertido com movimentação `IN` para cada item.

**Auth:** GERENTE_ESTOQUE, ADMIN

**Request:** sem body

**Response 200:**
```json
{
  "id": "uuid",
  "status": "PROCESSADO",
  "dataProcessamento": "2025-06-30T15:30:00",
  ...
}
```

**Erros:**
- `400` — devolução não está `PENDENTE` (já processada ou rejeitada)

---

### PATCH /api/returns/{id}/reject

Rejeita a devolução sem alterar o estoque.

**Auth:** GERENTE_ESTOQUE, ADMIN

**Request:** sem body

**Response 200:**
```json
{
  "id": "uuid",
  "status": "REJEITADO",
  "dataProcessamento": "2025-06-30T15:35:00",
  ...
}
```

**Erros:**
- `400` — devolução não está `PENDENTE`

---

## Comissões

### GET /api/commissions

Lista comissões de vendedores.

**Auth:** ADMIN

**Query params:**
- `status` — `PENDENTE` ou `PAGO` (opcional)
- `page`, `size`

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "vendedorId": "uuid",
      "vendedorNome": "Vendedor Um",
      "periodoInicio": "2025-06-01",
      "periodoFim": "2025-06-30",
      "totalPedidos": 18,
      "valorVendido": 32500.00,
      "quantidadeUnidades": 420,
      "taxaComissao": 3.50,
      "valorComissao": 1137.50,
      "status": "PENDENTE"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

`valorComissao` é calculado pelo banco: `valorVendido × taxaComissao / 100` (coluna `GENERATED STORED`).

---

## Performance de vendedores

### GET /api/sellers/performance

Performance do vendedor autenticado no mês corrente. Considera apenas pedidos com status `RETIRADO`.

**Auth:** VENDEDOR, ADMIN

**Response 200:**
```json
{
  "vendedorId": "uuid",
  "vendedorNome": "Vendedor Um",
  "vendedorEmail": "vendedor1@distribuidor.com",
  "totalPedidos": 18,
  "valorVendido": 32500.00,
  "quantidadeUnidades": 420
}
```

---

### GET /api/sellers/performance/all

Performance de todos os vendedores ativos no mês corrente.

**Auth:** ADMIN

**Response 200:** `List<SellerPerformanceResponse>`

---

## Usuários

### GET /api/users

Lista todos os usuários com paginação.

**Auth:** ADMIN

**Query params:** `page`, `size`

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "email": "admin@distribuidor.com",
      "nome": "Administrador",
      "role": "admin",
      "ativo": true,
      "telefone": null,
      "createdAt": "2025-01-01T00:00:00"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

### GET /api/users/{id}

Detalhe de um usuário.

**Auth:** ADMIN

**Response 200:** `UserResponse`

**Erros:**
- `404` — usuário não encontrado

---

### PATCH /api/users/{id}

Atualiza nome, telefone, role e status ativo do usuário.

**Auth:** ADMIN

**Request:**
```json
{
  "nome": "João Atualizado",
  "telefone": "11900001111",
  "role": "vendedor",
  "ativo": true
}
```

**Campos `role` aceitos:** `admin`, `gerente_estoque`, `vendedor`

**Response 200:** `UserResponse`

**Erros:**
- `400` — role inválida
- `404` — usuário não encontrado

---

## Formato de erro

Todos os erros seguem o mesmo envelope:

```json
{
  "error": "Descrição do erro",
  "status": 400
}
```

**Códigos HTTP usados:**

| Código | Quando |
|---|---|
| `400 Bad Request` | Validação falhou, transição de status inválida, estoque insuficiente |
| `401 Unauthorized` | Token ausente, inválido ou usuário inativo |
| `403 Forbidden` | Usuário autenticado mas sem a role necessária |
| `404 Not Found` | Recurso não encontrado |
| `409 Conflict` | Violação de unicidade (e-mail ou SKU duplicado) |
| `429 Too Many Requests` | Rate limit de login atingido |

Para erros de validação de campo (`400`), o campo `error` pode listar os campos problemáticos:
```json
{
  "error": "nome: não deve estar em branco; sku: não deve estar em branco",
  "status": 400
}
```

---

## Paginação

Endpoints que retornam listas usam o formato de página do Spring Data:

```json
{
  "content": [ ... ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false,
  "empty": false
}
```

**Parâmetros de paginação:**
- `page` — número da página, base 0 (default: `0`)
- `size` — itens por página (default: `20`)
- `sort` — campo e direção, ex.: `createdAt,desc` ou `nome,asc`
