# Configuração (application.yml)

Referência consolidada de todas as propriedades de configuração da aplicação. Cada seção remete ao guia de ferramenta correspondente para detalhes de uso.

## Banco de dados (`spring.datasource`)
| Variável de ambiente | Default (dev) | Descrição |
|---|---|---|
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `medstorage` | Nome do banco |
| `DB_USER` | `medstorage` | Usuário |
| `DB_PASSWORD` | `medstorage` | Senha |

Ver `docs/ferramentas/docker.md` (como rodar o banco local) e `docs/ferramentas/flyway.md` (migrations).

## JPA/Hibernate (`spring.jpa`)
- `ddl-auto: validate` — fixo, não configurável por ambiente. O Hibernate nunca cria/altera schema; só o Flyway é dono disso (ver `docs/decisoes-tecnicas/0001-ferramenta-de-migration.md`).
- `open-in-view: false` — desabilita o padrão "Open Session in View" do Spring Boot (evita acesso lazy a entidades fora da camada de serviço, força DTOs explícitos).

## Flyway (`spring.flyway`)
- `locations: classpath:db/migration`
- Ver `docs/ferramentas/flyway.md`, incluindo a pegadinha do Spring Boot 4 (dependência `spring-boot-flyway` separada).

## Email (`mailtrap.*`, `app.mail.*`)
| Variável de ambiente | Default (dev) | Descrição |
|---|---|---|
| `MAILTRAP_API_TOKEN` | _(vazio)_ | Token da API de Sending do Mailtrap |
| `MAIL_FROM` | `no-reply@distribuidor.com` | Remetente usado em `app.mail.from` |
| `MAIL_FROM_NAME` | `MedStorage` | Nome de exibição usado em `app.mail.from-name` |

Sem token real, o envio falha (erro de autenticação na API) — isso é esperado em dev e é capturado/logado, nunca quebra a aplicação. Ver `docs/ferramentas/mailtrap.md` e `docs/decisoes-tecnicas/0005-email-fora-da-transacao-de-pedido.md`.

## JWT (`jwt.*`, propriedade própria da aplicação)
| Variável de ambiente | Default (dev) | Descrição |
|---|---|---|
| `JWT_SECRET` | string de desenvolvimento (não usar em produção) | Chave de assinatura HS384. **Trocar obrigatoriamente em produção.** |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Validade do token |

Ver `docs/auth/README.md`.

## Variáveis usadas só pelo `docker-compose.yml`
`DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT` — as mesmas da seção de banco, lidas de um arquivo `.env` (não committado — ver `.env.example`) pelo Docker Compose para inicializar o container Postgres com as mesmas credenciais que a aplicação espera.

## Onde definir essas variáveis
- **Desenvolvimento local:** arquivo `.env` na raiz (copiar de `.env.example`), lido pelo `docker-compose.yml`. A aplicação Java em si só lê variáveis de ambiente reais do processo — se for rodar `./gradlew bootRun` fora do Docker, exporte as mesmas variáveis no shell ou aceite os defaults (que já apontam para o `docker-compose.yml` local).
- **CI:** definidas diretamente no workflow `.github/workflows/ci.yml` (valores fixos de teste, nunca secretos reais).
- **Produção:** variáveis de ambiente do provedor de deploy (Render, etc.) — nunca commitadas.
