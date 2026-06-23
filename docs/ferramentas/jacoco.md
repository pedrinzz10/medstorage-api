# Jacoco

## O que é
Ferramenta de medição de cobertura de testes para JVM — mede quais linhas/branches do código foram efetivamente exercitadas pelos testes automatizados.

## Por que usamos
Critério de aceitação geral do projeto (`docs/specs/01-overview.md`, `docs/specs/05-roadmap.md`): cobertura ≥ 80% por sprint. O Jacoco torna isso um gate automático no CI, não uma checagem manual.

## Setup neste projeto
Plugin no `build.gradle`:
```gradle
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = '0.8.12'
}
```

Duas tasks relevantes, encadeadas automaticamente após `test`:
- `jacocoTestReport` — gera relatório HTML (`build/reports/jacoco/test/html/index.html`) e XML
- `jacocoTestCoverageVerification` — **falha o build** se a cobertura de linha ficar abaixo de `0.80` (configurado em `violationRules`)

`check.dependsOn jacocoTestCoverageVerification` — ou seja, `./gradlew build` já roda a verificação automaticamente.

## Classes excluídas da cobertura
Configurado via `jacocoCoverageExclusions` no `build.gradle`:
- `**/*Application*` — classe principal do Spring Boot, não tem lógica para testar
- `**/*Config*` — classes de configuração (ex.: `SecurityConfig`), tipicamente só fiação de beans

Essa lista deve ser revisada conforme o projeto cresce — não usar como desculpa para excluir código que de fato tem lógica de negócio.

## Comandos do dia a dia
```bash
./gradlew test jacocoTestReport              # gera o relatório sem falhar o build
./gradlew jacocoTestCoverageVerification     # só a verificação (falha se < 80%)
./gradlew build                              # já inclui tudo isso
```

Abrir o relatório HTML localmente: `build/reports/jacoco/test/html/index.html`.
