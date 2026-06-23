# 04. Regras de Negócio e Casos de Erro

## Regras de negócio
1. **Workflow de status é unidirecional**: `PENDENTE → ATENDIDO → RETIRADO`. Nunca regride.
2. **Estoque não pode ficar negativo.** Ao marcar ATENDIDO com estoque insuficiente: rejeitar com 400 (oversell explícito é decisão de produto futura, não MVP).
3. **Desconto máximo de 50%** sobre o valor do pedido — validado no backend, nunca confiar no frontend.
4. **Email deve ser enviado ao marcar ATENDIDO.** Se falhar, registrar a tentativa e permitir retry (fila simples ou flag de reenvio).
5. **Comissão é calculada sobre valor vendido**, percentual configurável por vendedor; só contabiliza pedidos `RETIRADO`.
6. **Devolução só é permitida em pedidos `RETIRADO`.** Reverte estoque automaticamente (tipo `IN`, motivo "Devolução Pedido #X").
7. **Auditoria obrigatória**: toda transição de status e todo movimento de estoque registra `criado_por`/`processado_por` + timestamp.
8. **Mudança de status para ATENDIDO é transação atômica**: decremento de estoque + criação de `inventory_movement` + atualização de `orders` ocorrem juntos ou nada ocorre (rollback em caso de erro).

## Casos de erro mapeados

| Cenário | Resposta esperada |
|---|---|
| Criar pedido sem cliente | 400 + mensagem |
| Criar pedido sem itens | 400 + mensagem |
| Quantidade ≤ 0 em item | 400, bloqueado também no form |
| Marcar ATENDIDO com estoque insuficiente | 400, `"Insufficient stock for product X"` |
| Email falha ao enviar | Log de erro + retry, não bloqueia o fluxo de status |
| Serviço de estoque indisponível | Aviso ao gerente, não bloqueia |
| Usuário sem permissão (papel incorreto) | 403 |
| Pedido/cliente/produto não encontrado | 404 |
| Devolução solicitada em pedido não RETIRADO | 400 |

## Segurança
- JWT para autenticação; HTTPS obrigatório em produção
- Validação sempre no backend (Bean Validation + regras de serviço)
- Rate limiting em login: máx. 5 tentativas (a definir mecanismo — ver roadmap)
- Senhas com bcrypt (hash + salt)
- Auditoria completa de quem fez o quê e quando

## Performance (MVP, <50 pedidos/dia)
- PostgreSQL padrão é suficiente; cache (Redis) é opcional, não obrigatório no MVP
- Paginação padrão: 20 itens/página
- Índices obrigatórios: `customer_id`, `created_at`, `status` (ver `02-data-model.md`)
