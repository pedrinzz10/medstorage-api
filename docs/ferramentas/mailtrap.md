# Email — Mailtrap (Sending API / SDK Java)

## O que é
Mailtrap **Sending** é a API transacional de envio de email do Mailtrap — diferente do Email Testing/sandbox, aqui o email é entregue de verdade ao destinatário. Usamos o SDK oficial `io.mailtrap:mailtrap-java`, que fala com a API HTTP do Mailtrap (não SMTP).

## Por que usamos
Regra de negócio US-011: ao marcar um pedido como ATENDIDO, o sistema precisa notificar o cliente por email. Decisão registrada em `docs/decisoes-tecnicas/0003-envio-de-email.md`.

## Setup neste projeto
Dependência no `build.gradle`:
```gradle
implementation 'io.mailtrap:mailtrap-java:1.3.0'
```

Configuração (`application.yml`), usando variáveis de ambiente — nunca commitar credenciais:
```yaml
mailtrap:
  api-token: ${MAILTRAP_API_TOKEN:}

app:
  mail:
    from: ${MAIL_FROM:no-reply@distribuidor.com}
    from-name: ${MAIL_FROM_NAME:MedStorage}
```

O bean `MailtrapClient` é criado em `config/MailtrapClientConfig.java` a partir do token, e injetado em `OrderNotificationService`, que monta o `MailtrapMail` (from/to/subject/text/category) e chama `client.send(mail)`.

## Como obter as credenciais
1. Criar conta em [mailtrap.io](https://mailtrap.io)
2. No painel, ir em **Sending** → escolher/criar um **sending domain**
3. Na aba do domínio, gerar um **API Token** (em "API Tokens" ou na tela de integração)
4. Guardar como variável de ambiente local no `.env` (nunca no `application.yml` direto):
   ```
   MAILTRAP_API_TOKEN=<seu token>
   ```

## Valores que você precisa substituir antes de rodar
| Placeholder | Onde | O que colocar |
|---|---|---|
| `MAILTRAP_API_TOKEN` (`.env`) | token da API de Sending | gerado no passo 3 acima — **obrigatório**, sem ele todo envio falha (capturado e logado, não quebra a app) |
| `MAIL_FROM` (`.env`, opcional) | endereço do remetente | precisa ser um endereço de um **domínio verificado** no seu sending domain do Mailtrap. Durante testes na conta trial, o Mailtrap costuma fornecer um domínio de demonstração (ex.: `algo@demomailtrap.co`, visível no painel de Sending) que só envia para o email cadastrado na sua conta — use-o até verificar seu próprio domínio |
| `MAIL_FROM_NAME` (`.env`, opcional) | nome de exibição do remetente | tem default `MedStorage`, ajuste se quiser outro nome |

## Como checar o que foi enviado
Toda mensagem enviada (sucesso ou falha) aparece em **[mailtrap.io/sending/email_logs](https://mailtrap.io/sending/email_logs)** — útil para depurar sem precisar acessar a caixa de entrada do destinatário.

## Falhas e retry
Conforme `docs/specs/04-business-rules.md` regra 4: se o envio falhar (token inválido, domínio não verificado, etc.), `OrderNotificationService` captura a exceção, loga como warning e **não bloqueia** a transição de status do pedido. Não há fila de retry automática nesta sprint.
