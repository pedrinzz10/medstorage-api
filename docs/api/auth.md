# API — Autenticação

## POST /api/auth/login
**Request:**
```json
{ "email": "admin@distribuidor.com", "password": "Admin123!" }
```
**200 OK:**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "user": { "id": "uuid", "email": "admin@distribuidor.com", "nome": "Admin Master", "role": "admin" }
}
```
**401** — senha incorreta. **404** — usuário não existe. **400** — payload inválido (email ausente/mal formado, senha vazia).

## POST /api/auth/register
Requer header `Authorization: Bearer <token de admin>`.
**Request:**
```json
{ "email": "novo@distribuidor.com", "password": "senha123", "nome": "Novo Vendedor", "role": "vendedor", "telefone": "11999999999" }
```
**201 Created:**
```json
{ "id": "uuid", "email": "novo@distribuidor.com", "nome": "Novo Vendedor", "role": "vendedor" }
```
**400** — email já em uso, ou `role` fora de `vendedor`/`gerente_estoque`/`admin`. **401** — sem token. **403** — token válido mas papel ≠ admin.

## GET /api/auth/validate
Requer `Authorization: Bearer <token>`.
**200 OK:** `{ "valid": true, "email": "admin@distribuidor.com", "role": "admin" }`
**401** — sem token ou token inválido/expirado.

## POST /api/auth/refresh
Requer `Authorization: Bearer <token ainda válido>`. Retorna um novo token com a mesma identidade/papel.

## POST /api/auth/logout
Requer token válido. Sempre `200 OK` — operação no-op (ver `docs/auth/README.md`).
