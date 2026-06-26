# GitHub Actions (CI)

## O que é
Serviço de automação integrado ao GitHub que executa workflows (definidos em YAML) em resposta a eventos do repositório — neste projeto, a cada `push` ou `pull_request` para `main`/`develop`.

## Por que usamos
Para garantir que nenhum código quebrado ou sem cobertura de teste suficiente chegue em `main`/`develop`. Decisão registrada em `docs/decisoes-tecnicas/0004-estrategia-de-branching-e-cicd.md`.

## Onde está
`.github/workflows/ci.yml` — job único `build-and-test`:
1. Sobe um container de **PostgreSQL como serviço** (`services:` no workflow) — efêmero, só existe durante o job, não precisa do Docker Compose local
2. Configura JDK 21 (Temurin)
3. Roda `./gradlew build jacocoTestReport jacocoTestCoverageVerification`
4. Publica os relatórios de teste e cobertura como artefatos do job (acessíveis na aba "Actions" do GitHub, mesmo se o build falhar)

## Quando ele roda
- Em todo `pull_request` direcionado a `main` ou `develop`
- Em todo `push` direto a `main` ou `develop` (cobre o caso de merge de PR e back-merges de release/hotfix)

## Como ler um resultado
1. Abra o PR no GitHub → aba "Checks"
2. Se `build-and-test` falhar, abrir os logs do step que falhou (geralmente `Build, run tests and verify coverage`)
3. Os artefatos `jacoco-report` e `test-results` podem ser baixados para inspecionar localmente quais linhas/classes não tiveram cobertura

## Relação com a proteção de branch
O nome do check (`build-and-test`) é o que é marcado como **obrigatório** na proteção de `main`/`develop` — sem ele verde, o botão de merge do PR fica bloqueado no GitHub.
