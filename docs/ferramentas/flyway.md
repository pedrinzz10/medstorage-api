# Flyway

## O que é
Ferramenta de versionamento de schema de banco de dados. Em vez de alterar o banco manualmente, você escreve scripts SQL numerados ("migrations") e o Flyway aplica cada um exatamente uma vez, na ordem certa, em qualquer ambiente (dev, staging, produção).

## Por que usamos
O schema deste projeto tem 10 tabelas, views e triggers (ver `docs/specs/02-data-model.md`). Sem uma ferramenta de migration, seria preciso rodar SQL manualmente e torcer para que dev/staging/produção fiquem sincronizados. Decisão registrada em `docs/decisoes-tecnicas/0001-ferramenta-de-migration.md`.

## Como funciona
1. Toda migration vive em `src/main/resources/db/migration/`
2. O nome segue o padrão: `V{numero}__{descricao}.sql`
   - Exemplo: `V1__create_users_table.sql`, `V2__create_products_table.sql`
   - O número precisa ser sequencial e único; a descrição usa `_` no lugar de espaço
3. Quando a aplicação Spring Boot inicia, o Flyway:
   - Verifica a tabela `flyway_schema_history` no banco (cria se não existir)
   - Compara quais versões já foram aplicadas
   - Aplica, em ordem, os arquivos `V{n}__...sql` que ainda não rodaram
4. **Migration aplicada nunca é editada.** Se precisar corrigir algo, cria-se uma nova migration (`V5__fix_orders_constraint.sql`), nunca se edita `V2`.

## Setup neste projeto
Dependências no `build.gradle`:
```gradle
implementation 'org.springframework.boot:spring-boot-flyway'
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'
```

> **Pegadinha do Spring Boot 4:** até o Spring Boot 3, bastava adicionar `flyway-core` (+ `flyway-database-postgresql` para Postgres 10+) e a autoconfiguração já vinha embutida em `spring-boot-autoconfigure`. **A partir do Spring Boot 4, a autoconfiguração do Flyway foi extraída para um módulo próprio: `org.springframework.boot:spring-boot-flyway`.** Sem essa dependência explícita, o Flyway fica no classpath mas nunca é executado — nenhum erro aparece, as migrations simplesmente não rodam silenciosamente. Foi exatamente isso que aconteceu na Sprint 1 deste projeto: o build passava, mas a tabela `users` nunca era criada. O diagnóstico foi feito habilitando `debug: true` temporariamente no `application.yml` e lendo o "CONDITIONS EVALUATION REPORT" do Spring Boot — `FlywayAutoConfiguration` nem aparecia na lista (nem como match positivo, nem negativo), confirmando que a classe não estava disponível.

Configuração de conexão (`application.yml`) e localização das migrations:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
```

## Fluxo de trabalho no dia a dia
1. Preciso de uma tabela nova ou mudar uma coluna → crio um arquivo novo em `db/migration` com o próximo número
2. Rodo a aplicação (`./gradlew bootRun`) → Flyway aplica a migration automaticamente no banco local (Docker)
3. Confirmo no banco (ex.: `psql` ou um client gráfico) que a tabela/coluna foi criada
4. Commit do arquivo de migration junto com o código que depende dele

## Erros comuns
- **"Migration checksum mismatch"**: você editou um arquivo já aplicado. Solução: nunca editar, criar um novo.
- **Banco "sujo" em dev**: se quebrar tudo localmente, pode derrubar o container Docker e recriar do zero — em produção isso nunca é uma opção, só novas migrations corrigem.
- **Migrations não rodam e não há nenhum erro (Spring Boot 4+)**: falta a dependência `org.springframework.boot:spring-boot-flyway` (ver seção de Setup acima). Para confirmar o diagnóstico, habilite `debug: true` no `application.yml` temporariamente, rode a aplicação/testes e procure `FlywayAutoConfiguration` no "CONDITIONS EVALUATION REPORT" impresso no log — se não aparecer em nenhuma lista, a classe não está no classpath efetivo.

## Como validar que está funcionando
```bash
# depois de rodar a aplicação ou os testes:
docker exec <container-postgres> psql -U <user> -d <db> -c "\dt"
docker exec <container-postgres> psql -U <user> -d <db> -c "select version, description, success from flyway_schema_history;"
```
Deve aparecer a tabela `flyway_schema_history` e cada migration listada com `success = t`.
