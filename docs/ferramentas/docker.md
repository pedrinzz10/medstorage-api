# Docker (PostgreSQL local)

## O que é
Ferramenta para rodar serviços (como o PostgreSQL) em containers isolados, sem precisar instalar o banco direto na máquina.

## Por que usamos
Decisão registrada em `docs/decisoes-tecnicas/0002-banco-de-dados-dev.md`: banco de desenvolvimento roda em container local, sem depender de conta externa (Supabase fica reservado para staging/produção).

## Setup neste projeto
`docker-compose.yml` na raiz (criado na Sprint 1) define um serviço `postgres` com:
- Porta exposta (ex.: `5432:5432`)
- Variáveis de ambiente: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- Volume nomeado para persistir os dados entre reinicializações do container

## Comandos do dia a dia
```bash
docker compose up -d        # sobe o banco em background
docker compose down         # derruba o banco (mantém os dados, por causa do volume)
docker compose down -v      # derruba o banco E apaga os dados (reset completo)
docker compose logs -f postgres   # acompanha os logs do banco
```

## Conectar no banco rodando
```bash
docker exec -it <nome-do-container> psql -U <usuario> -d <database>
```

## Relação com o Spring Boot
A `application.yml` (ou `application-dev.yml`) aponta `spring.datasource.url` para `jdbc:postgresql://localhost:5432/<database>`, usando as mesmas credenciais do `docker-compose.yml`.
