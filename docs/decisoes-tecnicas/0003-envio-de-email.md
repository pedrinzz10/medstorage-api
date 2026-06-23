# 0003 — Envio de email de notificação

## Contexto
Regra de negócio exige envio de email ao cliente quando um pedido é marcado como ATENDIDO (US-011).

## Decisão
Usar `spring-boot-starter-mail` configurado com SMTP do **Mailgun** (free tier/sandbox para desenvolvimento).

## Alternativas consideradas
- **Mailtrap/Mailpit local** — bom para inspecionar emails em dev sem enviar de verdade, mas exige configurar outro provedor depois para produção.
- **SMTP real desde já (Gmail, SES, SendGrid)** — Mailgun foi escolhido por ter free tier acessível e já ser válido também para produção, evitando troca de provedor depois.

## Consequências
- Credenciais SMTP do Mailgun precisam ser configuradas como variáveis de ambiente (nunca hardcoded ou commitadas).
- Falha de envio deve ser logada e ter estratégia de retry (ver `docs/specs/04-business-rules.md`, regra 4).
- Documentação de uso em `docs/ferramentas/smtp-mailgun.md`.
