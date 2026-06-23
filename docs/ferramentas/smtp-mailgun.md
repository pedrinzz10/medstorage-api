# SMTP / Mailgun

## O que é
Mailgun é um provedor de envio de email transacional. O backend não fala direto com o cliente de email do destinatário — ele envia o email via protocolo SMTP para o Mailgun, que cuida da entrega.

## Por que usamos
Regra de negócio US-011: ao marcar um pedido como ATENDIDO, o sistema precisa notificar o cliente por email. Decisão registrada em `docs/decisoes-tecnicas/0003-envio-de-email.md`.

## Setup neste projeto
Dependência no `build.gradle`:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-mail'
```

Configuração (`application.yml`), usando variáveis de ambiente — nunca commitar credenciais:
```yaml
spring:
  mail:
    host: smtp.mailgun.org
    port: 587
    username: ${MAILGUN_SMTP_USER}
    password: ${MAILGUN_SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

## Como obter as credenciais
1. Criar conta no Mailgun (free tier/sandbox)
2. No painel, criar um domínio (ou usar o sandbox domain fornecido)
3. Em "SMTP credentials", gerar usuário/senha SMTP
4. Guardar como variáveis de ambiente locais (`.env`, nunca no `application.yml` direto)

## Uso no código
`JavaMailSender` (autoconfigurado pelo starter) é injetado no service responsável por notificações, ex. `OrderNotificationService`, chamado dentro da transação de `markAsAttended`.

## Falhas e retry
Conforme `docs/specs/04-business-rules.md` regra 4: se o envio falhar, registrar a tentativa (log estruturado) e não bloquear a transição de status do pedido — o email não deve impedir o fluxo principal do negócio.
