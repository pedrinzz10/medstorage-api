# 0003 — Envio de email de notificação

## Contexto
Regra de negócio exige envio de email ao cliente quando um pedido é marcado como ATENDIDO (US-011).

## Decisão
Usar o **SDK Java oficial do Mailtrap** (`io.mailtrap:mailtrap-java`), que fala com a API HTTP de **Sending** do Mailtrap, para envio real de email transacional.

## Histórico
1. Inicialmente foi escolhido o Mailgun via `spring-boot-starter-mail`/SMTP (free tier, válido para produção).
2. Trocado para Mailtrap **Email Testing** (SMTP sandbox) para evitar enviar para endereços reais durante dev/teste.
3. Trocado novamente para Mailtrap **Sending** via SDK Java — a API oficial tem respostas estruturadas (em vez de só exceções de SMTP), suporta metadados (`category`) úteis para os logs em `mailtrap.io/sending/email_logs`, e elimina a dependência de `spring-boot-starter-mail`.

## Alternativas consideradas
- **SMTP (Mailgun ou Mailtrap sandbox)** — funciona, mas perde os logs estruturados e metadados que a API de Sending oferece.

## Consequências
- O token da API de Sending do Mailtrap precisa ser configurado como variável de ambiente (nunca hardcoded ou commitada) — `MAILTRAP_API_TOKEN`.
- `app.mail.from` precisa ser um endereço de um domínio verificado no Mailtrap (ou o domínio de demonstração da conta trial) — não é mais necessário configurar SMTP host/port.
- O código (`OrderNotificationService`) agora depende diretamente do `MailtrapClient` (bean em `config/MailtrapClientConfig.java`) em vez do `JavaMailSender` do Spring — trocar de provedor exigiria reescrever esse service, não é mais uma simples troca de variáveis de ambiente.
- Falha de envio deve ser logada e não bloquear a transição de status do pedido (ver `docs/specs/04-business-rules.md`, regra 4).
- Documentação de uso em `docs/ferramentas/mailtrap.md`.
