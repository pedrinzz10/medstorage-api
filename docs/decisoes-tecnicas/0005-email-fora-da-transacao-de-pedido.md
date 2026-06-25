# 0005 — Email de notificação enviado após o commit da transação

## Contexto
Regra de negócio (US-011 / `docs/specs/04-business-rules.md` regra 4): ao marcar um pedido como `ATENDIDO`, o cliente deve ser notificado por email, mas uma falha no envio **não pode** desfazer a mudança de status nem o decremento de estoque (que precisam ser atômicos entre si).

## Decisão
`OrderService.markAsAttended` é `@Transactional` e só contém as mutações de banco (estoque, movimento, status do pedido). Antes de retornar, registra um `TransactionSynchronization` via `TransactionSynchronizationManager`, cujo `afterCommit()` chama `OrderNotificationService.sendOrderReadyEmail`. Assim:
- Se a transação for revertida (ex.: estoque insuficiente), o email **nunca é enviado** — não há callback `afterCommit`.
- Se a transação for confirmada, o email é enviado **depois** que os dados já estão persistidos — uma falha de SMTP não afeta o que já foi salvo.
- Em contexto sem transação Spring ativa (chamada direta em teste unitário, fora de um proxy `@Transactional`), há um fallback que envia o email imediatamente, evitando `IllegalStateException` do `TransactionSynchronizationManager`.

## Alternativas consideradas
- **Enviar o email dentro do mesmo método, antes do fim da transação:** mais simples, mas prende a transação (e os locks de banco) durante uma chamada de rede (SMTP), além de arriscar enviar o email mesmo se algo falhar depois (já que a transação só comita ao final do método).
- **Extrair para um segundo bean e usar `@Transactional` próprio:** funcionaria, mas adicionaria uma classe só para evitar o problema de auto-invocação do Spring AOP — `TransactionSynchronizationManager` resolve isso sem nova abstração.

## Consequências
- Não há fila de retry automática para emails que falham — só log de warning (ver `docs/ferramentas/mailtrap.md`). Se isso se tornar um problema real em produção, considerar uma fila (ex.: tabela de "notificações pendentes" + job, ou um message broker).
- Qualquer novo fluxo que precise "fazer algo só se a transação principal for bem-sucedida, mas sem fazer parte da atomicidade dela" deve seguir o mesmo padrão.

## Achado relacionado: geração de `numero_pedido` movida para Java
Inicialmente `numero_pedido` era gerado por um trigger `BEFORE INSERT` no Postgres, com o campo mapeado via `@Generated(event = EventType.INSERT)` do Hibernate para reler o valor após o insert. Isso funcionou em uso real (`bootRun` + curl), mas se mostrou **não-determinístico dentro de testes `@Transactional` aninhados**: o PR #6 passou localmente várias vezes e falhou no CI com `numeroPedido` ausente da resposta. Em vez de depurar a fundo o timing exato do Hibernate, a correção foi gerar o número diretamente em Java (`OrderService.generateNumeroPedido`, lendo a mesma sequence via query nativa) antes do insert — elimina a dependência de releitura pós-insert para esse campo. O trigger continua no banco como rede de segurança para inserts diretos via SQL, mas não é mais o caminho usado pela aplicação.
