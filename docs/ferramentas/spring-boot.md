# Spring Boot

## O que é
Framework Java para construir aplicações web/APIs de forma produtiva, com configuração automática ("autoconfiguration") e um conjunto de bibliotecas integradas ("starters").

## Versão usada neste projeto
**Spring Boot 4.1.0**, Java 21, gerenciado via Gradle (`build.gradle`).

> Atenção: a documentação original do projeto (`docs/inicial_docs/`) foi escrita pensando em Spring Boot 3. Sempre validar exemplos de código contra a 4.1 antes de copiar.

> **Modularização no Spring Boot 4:** autoconfigurações que antes vinham todas dentro de `spring-boot-autoconfigure` foram quebradas em módulos próprios (ex.: `spring-boot-jdbc`, `spring-boot-jpa`, `spring-boot-hibernate`, `spring-boot-flyway`). Isso significa que adicionar só a biblioteca de terceiros (ex.: `flyway-core`) pode não ser suficiente — às vezes é preciso adicionar também o módulo `spring-boot-*` correspondente para a autoconfiguração funcionar. Ver exemplo real em `docs/ferramentas/flyway.md`.
>
> A modularização também afeta **pacotes de anotações de teste**: `@AutoConfigureMockMvc`, que em versões anteriores vivia em `org.springframework.boot.test.autoconfigure.web.servlet`, está em `org.springframework.boot.webmvc.test.autoconfigure` no Boot 4.1 (módulo `spring-boot-webmvc-test`). `MockMvc`, `MockMvcRequestBuilders` e `MockMvcResultMatchers` continuam em `org.springframework.test.web.servlet.*` (não mudaram, vêm do `spring-test`, não do Boot). Se um import de teste não resolver, vale checar se a classe migrou de pacote em vez de assumir que a dependência está faltando.

## Starters usados
| Starter | Para quê |
|---|---|
| `spring-boot-starter-data-jpa` | Persistência (Hibernate/JPA) — entidades, repositórios |
| `spring-boot-starter-security` | Autenticação/autorização (JWT, controle de acesso por papel) |
| `spring-boot-starter-validation` | Bean Validation (`@NotNull`, `@Email`, etc.) nos DTOs |
| `spring-boot-starter-webmvc` | Controllers REST (`@RestController`) |
| `spring-boot-devtools` | Hot reload em desenvolvimento |

## Conceitos-chave para este projeto
- **Camadas:** `Controller` (recebe HTTP) → `Service` (regra de negócio) → `Repository` (acesso a dados via JPA)
- **DTOs vs Entities:** nunca expor entidades JPA diretamente na API — usar DTOs de request/response
- **`@Transactional`:** usado nos services onde múltiplas operações precisam ser atômicas (ex.: marcar pedido ATENDIDO = atualizar status + decrementar estoque + registrar movimento, tudo ou nada)
- **`@ControllerAdvice`:** centraliza o tratamento de erros e o formato padronizado `{ "error": ..., "status": ... }`

## Comandos úteis
```bash
./gradlew build          # compila e roda testes
./gradlew bootRun        # roda a aplicação localmente
./gradlew test           # só os testes
```
