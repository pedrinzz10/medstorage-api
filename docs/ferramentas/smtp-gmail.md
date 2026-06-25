# SMTP / Gmail

## O que é
Envio de email transacional via SMTP do Gmail (`smtp.gmail.com`), usando `spring-boot-starter-mail`/`JavaMailSender` (autoconfigurado pelo Spring Boot a partir de `spring.mail.*`).

## Por que usamos
Regra de negócio US-011: ao marcar um pedido como ATENDIDO, o sistema precisa notificar o cliente por email. Decisão registrada em `docs/decisoes-tecnicas/0003-envio-de-email.md`.

> **Importante:** use sempre uma conta Gmail **dedicada para testes deste projeto**, nunca sua conta pessoal — qualquer vazamento de configuração (`.env`, log, captura de tela) exporia a conta real, não só um token de serviço descartável.

## Limites do Gmail SMTP
Conta gratuita (`@gmail.com`): **500 destinatários por período de 24 horas** (cada destinatário em cada envio conta, não a quantidade de mensagens). Suficiente para o volume de testes/uso acadêmico deste projeto — não é adequado para envio em produção/escala.

## Setup neste projeto
Dependência no `build.gradle` (já presente):
```gradle
implementation 'org.springframework.boot:spring-boot-starter-mail'
```

Configuração (`application.yml`), usando variáveis de ambiente para as credenciais — nunca commitar:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:} # pragma: allowlist secret
    password: ${MAIL_PASSWORD:} # pragma: allowlist secret
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

app:
  mail:
    from: ${MAIL_USERNAME:no-reply@distribuidor.com}
    from-name: ${MAIL_FROM_NAME:MedStorage}
```

O Gmail exige que o cabeçalho `From` corresponda à conta autenticada — por isso `app.mail.from` deriva de `MAIL_USERNAME` por padrão.

## Como obter as credenciais
1. Criar (ou usar) uma conta Gmail **dedicada para testes** deste projeto (ex.: `medstorage06@gmail.com`)
2. Ativar a verificação em duas etapas na conta (obrigatório para gerar senha de app)
3. Gerar uma **senha de app** em [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
4. Guardar como variáveis de ambiente locais no `.env` (nunca no `application.yml` direto, nunca em chat/commit):
   ```
   MAIL_USERNAME=<email da conta de teste>
   MAIL_PASSWORD=<senha de app gerada>
   ```

## Valores que você precisa substituir antes de rodar
| Placeholder | Onde | O que colocar |
|---|---|---|
| `MAIL_USERNAME` (`.env`) | endereço Gmail | a conta **dedicada de teste**, não a pessoal |
| `MAIL_PASSWORD` (`.env`) | senha de app | gerada no passo 3 acima — **nunca a senha normal da conta** |

## Falhas e retry
Conforme `docs/specs/04-business-rules.md` regra 4: se o envio falhar (credenciais inválidas, limite de 500/dia excedido, etc.), `OrderNotificationService` captura a `MailException`, loga como warning e **não bloqueia** a transição de status do pedido. Não há fila de retry automática nesta sprint.
