# SMTP / Mailtrap

## O que é
Mailtrap é um provedor de **email testing** (sandbox): o backend envia o email via SMTP normalmente, mas ele nunca chega numa caixa real — fica retido numa inbox virtual do Mailtrap, onde dá pra inspecionar o conteúdo (HTML, headers, spam score) sem risco de notificar um cliente de verdade durante dev/teste.

## Por que usamos
Regra de negócio US-011: ao marcar um pedido como ATENDIDO, o sistema precisa notificar o cliente por email. Para ambiente de desenvolvimento, queremos validar que o email é **montado e enviado corretamente** sem o risco de mandar algo para um endereço real (os emails de clientes seed/teste podem nem existir). Decisão atualizada em `docs/decisoes-tecnicas/0003-envio-de-email.md`.

> Em produção, será necessário trocar para um provedor de envio real (Mailgun, SES, SendGrid etc.) — o código (`OrderNotificationService`) é agnóstico de provedor, só depende do `JavaMailSender` autoconfigurado pelo Spring Boot a partir de `spring.mail.*`, então a troca é só de configuração/variáveis de ambiente.

## Setup neste projeto
Dependência no `build.gradle` (já presente, não muda):
```gradle
implementation 'org.springframework.boot:spring-boot-starter-mail'
```

Configuração (`application.yml`), usando variáveis de ambiente — nunca commitar credenciais:
```yaml
spring:
  mail:
    host: ${MAIL_SMTP_HOST:sandbox.smtp.mailtrap.io}
    port: ${MAIL_SMTP_PORT:587}
    username: ${MAIL_SMTP_USERNAME:}
    password: ${MAIL_SMTP_PASSWORD:}
```

## Como obter as credenciais
1. Criar conta em [mailtrap.io](https://mailtrap.io) (free tier serve)
2. No painel, ir em **Email Testing** → criar (ou usar a inbox "My Inbox" padrão)
3. Na aba **SMTP Settings** da inbox, copiar `Host`, `Port`, `Username` e `Password`
4. Guardar como variáveis de ambiente locais no `.env` (nunca no `application.yml` direto):
   ```
   MAIL_SMTP_HOST=sandbox.smtp.mailtrap.io
   MAIL_SMTP_PORT=587
   MAIL_SMTP_USERNAME=<seu usuario>
   MAIL_SMTP_PASSWORD=<sua senha>
   ```
5. Após rodar o fluxo de marcar um pedido como ATENDIDO, o email aparece na inbox do Mailtrap (não na caixa do cliente).

## Uso no código
`JavaMailSender` (autoconfigurado pelo starter) é injetado em `OrderNotificationService`, chamado dentro da transação de `markAsAttended`, mas só efetivamente enviado **depois do commit** (ver `docs/decisoes-tecnicas/0005-email-fora-da-transacao-de-pedido.md`).

## Falhas e retry
Conforme `docs/specs/04-business-rules.md` regra 4: se o envio falhar, registrar a tentativa (log estruturado) e não bloquear a transição de status do pedido — o email não deve impedir o fluxo principal do negócio. Não há fila de retry automática nesta sprint.
