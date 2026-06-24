# springdoc-openapi (Swagger)

## O que é
Biblioteca que gera documentação OpenAPI 3 automaticamente a partir das anotações Spring MVC (`@RestController`, `@GetMapping`, DTOs) já existentes no código — sem precisar escrever a especificação manualmente.

## Por que usamos
Critério de aceitação geral do projeto (`docs/specs/01-overview.md`): "Documentação de API via OpenAPI/Swagger". O `SecurityConfig` já liberava `/swagger-ui/**` e `/v3/api-docs/**` desde a Sprint 1, antecipando essa adição.

## Setup
Dependência no `build.gradle`:
```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5'
```

`OpenApiConfig` (`src/main/java/.../config/OpenApiConfig.java`) define título/descrição/versão e registra o esquema de segurança `bearerAuth`, para o botão **Authorize** do Swagger UI aceitar o JWT direto (sem precisar prefixar `Bearer ` manualmente).

## Onde acessar
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- JSON OpenAPI puro: `http://localhost:8080/v3/api-docs`

Ambos são públicos (`permitAll` no `SecurityConfig`) — é só documentação, não expõe dados.

## Como testar um endpoint autenticado direto no Swagger UI
1. Faça login via `POST /api/auth/login` no próprio Swagger UI (ou via curl) e copie o `token`
2. Clique em **Authorize** (canto superior direito) e cole só o token (sem `Bearer `, o springdoc já adiciona o prefixo)
3. Os demais endpoints passam a incluir o header `Authorization` automaticamente

## Limitação atual
A documentação gerada usa só os tipos/anotações Spring MVC existentes (nomes de parâmetros, DTOs, status codes mapeados pelo `GlobalExceptionHandler` não aparecem automaticamente nas respostas de erro). Anotações `@Operation`/`@ApiResponse` detalhadas por endpoint não foram adicionadas — ver `docs/specs/05-roadmap.md` Sprint 8 ("Documentação, hardening e fechamento") para um possível polimento futuro, se necessário.
