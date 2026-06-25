# 0003 — Envio de email de notificação

## Contexto
Regra de negócio exige envio de email ao cliente quando um pedido é marcado como ATENDIDO (US-011).

## Decisão
Usar `spring-boot-starter-mail` configurado com SMTP do **Mailtrap** (Email Testing/sandbox) para desenvolvimento e testes.

## Histórico
Inicialmente foi escolhido o Mailgun (free tier, já válido para produção). Trocado para Mailtrap porque, em dev, os emails de clientes seed/teste podem não existir de fato — o Mailtrap retém o email numa inbox virtual em vez de tentar entregá-lo, eliminando o risco de enviar para um endereço real por engano durante testes manuais/CI.

## Alternativas consideradas
- **Mailgun** — válido para produção, mas em dev não há proteção contra envio acidental para um endereço real.
- **SMTP real desde já (Gmail, SES, SendGrid)** — mesmo risco do Mailgun em ambiente de dev/teste.

## Consequências
- Credenciais SMTP do Mailtrap precisam ser configuradas como variáveis de ambiente (nunca hardcoded ou commitadas) — `MAIL_SMTP_HOST/PORT/USERNAME/PASSWORD`.
- O código (`OrderNotificationService`) é agnóstico de provedor — só depende de `JavaMailSender`/`spring.mail.*` — então produção poderá trocar para um provedor de envio real (Mailgun, SES, etc.) só mudando variáveis de ambiente, sem alterar código.
- Falha de envio deve ser logada e não bloquear a transição de status do pedido (ver `docs/specs/04-business-rules.md`, regra 4).
- Documentação de uso em `docs/ferramentas/smtp-mailtrap.md`.
