# 0002 — Banco de dados em desenvolvimento

## Contexto
A documentação original sugeria Supabase como banco gerenciado. Para desenvolvimento, é preciso decidir onde o PostgreSQL roda.

## Decisão
PostgreSQL via **Docker local**, definido em `docker-compose.yml` na raiz do projeto.

## Alternativas consideradas
- **Supabase** — útil para staging/produção (backups automáticos, point-in-time recovery), mas adiciona dependência de internet/conta externa só para desenvolver localmente.

## Consequências
- Ambiente de dev não depende de internet nem de conta externa.
- Supabase (ou outro Postgres gerenciado) pode ser usado em staging/produção sem mudar o código — só a `application.yml`/variáveis de ambiente de conexão.
- Documentação de uso em `docs/ferramentas/docker.md`.
