# Documentação — MedStorage API

## Estrutura

| Pasta/Arquivo | Conteúdo |
|---|---|
| `configuracao.md` | Referência consolidada de todas as propriedades de `application.yml` e variáveis de ambiente |
| `inicial_docs/` | Documentação original recebida (requisitos, schema, guia de backend) — fonte histórica, não editar |
| `specs/` | Specs técnicas consolidadas (visão, modelo de dados, contrato de API, regras de negócio, roadmap) — referência de planejamento |
| `auth/` | Documentação de autenticação/autorização: JWT, papéis, Spring Security |
| `api/` | Documentação dos endpoints conforme são implementados (request/response reais, exemplos testados) |
| `db/` | Documentação do banco: migrations aplicadas, mudanças de schema, queries relevantes |
| `decisoes-tecnicas/` | Registro de decisões técnicas (formato ADR — Architecture Decision Record) |
| `ferramentas/` | Explicações sobre as ferramentas usadas no projeto (Spring Boot, Flyway, Docker, SMTP, etc.) — guias de uso e conceitos |

## Convenção de atualização

Cada vez que uma tarefa de implementação for concluída, a documentação correspondente é criada ou atualizada na pasta da área afetada:
- Mudou algo de autenticação/segurança → `auth/`
- Novo endpoint ou mudança de contrato → `api/`
- Nova migration ou mudança de schema → `db/`
- Decisão de arquitetura/ferramenta tomada → `decisoes-tecnicas/` (novo ADR)
- Ferramenta nova introduzida → `ferramentas/` (novo guia)

As specs em `specs/` representam o planejamento inicial e não são reescritas a cada tarefa — servem de referência. A documentação "viva" do que foi de fato implementado vive nas pastas por área.
