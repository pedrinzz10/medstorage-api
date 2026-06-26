# 0004 — Estratégia de branching e CI/CD

## Contexto
O projeto precisa de um processo formal antes de qualquer código novo ser mesclado na `main`: nunca commitar direto na `main`, sempre passar por branch + Pull Request + CI verde + aprovação.

## Decisão
Adotar **Git Flow completo**:

| Branch | Papel | Protegida? |
|---|---|---|
| `main` | Código em produção. Só recebe merge de `release/*` ou `hotfix/*` | Sim |
| `develop` | Integração contínua de features. Só recebe merge de `feature/*`, `release/*` (back-merge) ou `hotfix/*` | Sim |
| `feature/*` | Uma tarefa/sprint isolada, criada a partir de `develop` | Não |
| `release/*` | Preparação de uma versão, criada a partir de `develop`, mesclada em `main` e back-merged em `develop` | Não |
| `hotfix/*` | Correção urgente em produção, criada a partir de `main`, mesclada em `main` e em `develop` | Não |

Convenção de nomes: `feature/sprint-1-setup`, `feature/sprint-1-auth`, `release/0.1.0`, `hotfix/fix-login-bug`.

## CI (GitHub Actions)
Workflow `.github/workflows/ci.yml`, disparado em `pull_request` e `push` para `main` e `develop`:
1. Sobe um PostgreSQL de serviço (container efêmero do próprio job)
2. `./gradlew build jacocoTestReport jacocoTestCoverageVerification`
3. Publica relatório de cobertura (Jacoco) e resultados de teste como artefatos do job

## Proteção de branch (GitHub)
Em `main` e `develop`:
- Bloqueado push direto (inclusive para administradores)
- PR obrigatório
- Status check `build-and-test` obrigatório e verde
- Pelo menos 1 aprovação antes de mergear
- Branch precisa estar atualizada com a base antes do merge

## Alternativas consideradas
- **GitHub Flow** (só `main` + branches de feature) — mais simples, mas o usuário optou pelo Git Flow completo para ter um processo claro de releases mesmo em um projeto solo.

## Consequências
- Todo trabalho passa a ser feito em `feature/*`, nunca direto em `develop`/`main`.
- `main` só evolui via `release/*`/`hotfix/*`, nunca recebe `feature/*` diretamente.
- Cobertura de testes abaixo de 80% (excluindo classes de configuração/`*Application*`) **quebra o build no CI**, bloqueando o merge.
