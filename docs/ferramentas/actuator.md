# Spring Boot Actuator

## O que é
Conjunto de endpoints de operação/observabilidade que o Spring Boot expõe automaticamente (health checks, métricas, info) a partir da dependência `spring-boot-starter-actuator`.

## Por que usamos
Recomendação geral de boas práticas Spring Boot (skill `spring-boot`: "Leverage Spring Boot Actuator for health checks, metrics, and monitoring"). Health check é pré-requisito comum para load balancers e plataformas de deploy (Render, Kubernetes, etc.) saberem se a instância está pronta para receber tráfego.

## Setup
```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

Endpoint exposto publicamente (sem autenticação) no `SecurityConfig`:
```java
.requestMatchers("/actuator/health/**").permitAll()
```
Por padrão, só `/actuator/health` é exposto via web — nenhum outro endpoint do Actuator (env, beans, etc.) fica acessível sem configuração explícita adicional, o que evita expor informação sensível.

## Pegadinha encontrada: Mail Health Indicator
Por padrão, com `spring-boot-starter-mail` no classpath, o Actuator registra automaticamente um `MailHealthIndicator` que **testa a conexão SMTP real** a cada chamada de `/actuator/health`. Como o ambiente de dev/CI não tem credenciais reais do Mailgun, isso marcava a aplicação inteira como `DOWN` — mesmo com banco, JPA e tudo mais funcionando perfeitamente.

Isso contradiz a decisão de `docs/decisoes-tecnicas/0005-email-fora-da-transacao-de-pedido.md`: falha de email é best-effort, não deveria afetar a operação principal nem a saúde reportada do serviço. Corrigido em `application.yml`:
```yaml
management:
  health:
    mail:
      enabled: false
```

## Como verificar
```bash
curl http://localhost:8080/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
```
