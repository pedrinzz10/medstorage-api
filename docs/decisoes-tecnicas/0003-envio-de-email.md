# 0003 — Envio de email de notificação

## Contexto
Regra de negócio exige envio de email ao cliente quando um pedido é marcado como ATENDIDO (US-011).

## Decisão
Usar `spring-boot-starter-mail` configurado com SMTP do **Gmail** (`smtp.gmail.com`), com uma conta dedicada criada só para testes deste projeto.

## Histórico
1. Inicialmente foi escolhido o Mailgun (free tier/sandbox).
2. Avaliada a troca para a Mailtrap Sending API (SDK Java, envio real com dashboard de logs dedicado) — chegou a ser implementada num PR, mas não foi seguida adiante.
3. Decisão final: **Gmail SMTP**, com conta dedicada de teste (nunca a pessoal). Motivo: o limite gratuito do Gmail (500 destinatários/24h) é suficiente para o volume de testes deste projeto acadêmico, e a configuração é mais simples (sem verificação de domínio de envio, como exigiria um provedor transacional em produção).

## Alternativas consideradas
- **Mailgun / Mailtrap Sending API** — provedores transacionais dedicados, com dashboards de log e limites maiores, mas exigem verificação de domínio de envio para uso real — desnecessário para o escopo de testes deste projeto.

## Consequências
- Credenciais SMTP do Gmail (`MAIL_USERNAME`, `MAIL_PASSWORD` — senha de app, não a senha normal da conta) precisam ser configuradas como variáveis de ambiente (nunca hardcoded ou commitadas, nunca coladas em chat).
- `app.mail.from` deriva de `MAIL_USERNAME` por padrão, pois o Gmail exige que o remetente corresponda à conta autenticada.
- Limite de 500 destinatários/24h — adequado para testes, não para produção em escala.
- Falha de envio deve ser logada e não bloquear a transição de status do pedido (ver `docs/specs/04-business-rules.md`, regra 4).
- Documentação de uso em `docs/ferramentas/smtp-gmail.md`.
