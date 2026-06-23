# Git Flow

## O que é
Um modelo de branches que organiza o trabalho em torno de papéis fixos: `main` (produção), `develop` (integração), e branches temporárias (`feature/*`, `release/*`, `hotfix/*`) que nascem de um branch e morrem ao serem mescladas. Decisão registrada em `docs/decisoes-tecnicas/0004-estrategia-de-branching-e-cicd.md`.

## Regra de ouro
**Nunca commitar direto em `main` ou `develop`.** Todo trabalho nasce em uma branch temporária e entra via Pull Request, depois de CI verde + aprovação.

## Os 5 tipos de branch

| Branch | Nasce de | Vai para | Quando usar |
|---|---|---|---|
| `main` | — | — | Sempre o código que está/vai para produção |
| `develop` | `main` (uma vez, no início) | — | Integração contínua das features |
| `feature/<nome>` | `develop` | `develop` | Uma tarefa, sprint ou pedaço de funcionalidade |
| `release/<versao>` | `develop` | `main` + back-merge em `develop` | Empacotar um conjunto de features para lançar |
| `hotfix/<nome>` | `main` | `main` + `develop` | Bug urgente em produção que não pode esperar a próxima release |

## Fluxo do dia a dia (feature)
```bash
git checkout develop
git pull origin develop
git checkout -b feature/sprint-1-setup

# ... trabalha, commita ...

git push -u origin feature/sprint-1-setup
gh pr create --base develop --title "Sprint 1: docker-compose, Flyway e setup" --body "..."
# espera CI passar + aprovação
# merge via GitHub (squash ou merge commit, conforme preferência do time)
```

## Fluxo de release
```bash
git checkout develop
git pull origin develop
git checkout -b release/0.1.0

# ajustes finais, bump de versão, changelog

git push -u origin release/0.1.0
gh pr create --base main --title "Release 0.1.0"
# depois do merge em main: back-merge em develop
git checkout develop
git merge main
git push origin develop
```

## Fluxo de hotfix
```bash
git checkout main
git checkout -b hotfix/fix-login-bug

# corrige o bug

git push -u origin hotfix/fix-login-bug
gh pr create --base main --title "Hotfix: corrige bug de login"
# depois do merge em main, também precisa ir para develop:
git checkout develop
git merge main
git push origin develop
```

## Convenção de nomes
- `feature/sprint-1-setup`, `feature/sprint-4-marcar-atendido`
- `release/0.1.0` (semver)
- `hotfix/corrige-decremento-estoque`

## Proteção de branch
`main` e `develop` são protegidas no GitHub: sem push direto, PR obrigatório, CI (`build-and-test`) precisa passar, e é necessária pelo menos 1 aprovação antes do merge.
