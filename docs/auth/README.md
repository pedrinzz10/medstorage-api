# Autenticação e Autorização

## Implementado (Sprint 1)
- Entidade `User` (`domain/user/User.java`) com `role` mapeado via `UserRoleConverter` para os valores do banco (`vendedor`, `gerente_estoque`, `admin`)
- `JwtProvider` (`security/JwtProvider.java`): gera e valida tokens HS384 (lib `io.jsonwebtoken:jjwt`), claims `sub` (email), `userId`, `role`, expiração configurável
- `JwtAuthenticationFilter`: lê o header `Authorization: Bearer <token>`, valida e popula o `SecurityContext` com as `GrantedAuthority` no formato `ROLE_<PAPEL>`
- `SecurityConfig`: sessão stateless, CORS liberado, `/api/auth/login` público, `/api/auth/register` exige `ROLE_ADMIN`, todo o resto exige autenticação. Requisição sem token autenticado retorna **401** (customizado via `authenticationEntryPoint`, já que o padrão do Spring Security seria 403)
- `AuthService` + `AuthController`: login, registro (admin-only), validação de token, refresh, logout

## Endpoints implementados

| Endpoint | Autenticação | Observação |
|---|---|---|
| `POST /api/auth/login` | Pública | 200 + token/usuário, 401 senha errada, 404 usuário não existe |
| `POST /api/auth/register` | `ROLE_ADMIN` | 201 + usuário criado, 400 email duplicado/role inválida |
| `GET /api/auth/validate` | Qualquer usuário autenticado | 200 com `{valid, email, role}` |
| `POST /api/auth/refresh` | Qualquer usuário autenticado | Reemite token a partir de um token ainda válido |
| `POST /api/auth/logout` | Qualquer usuário autenticado | No-op — ver nota abaixo |

> **Nota sobre logout:** como o JWT é stateless (sem sessão no servidor), não há nada para "invalidar" no backend. O endpoint existe para compatibilidade com o contrato de API, mas a invalidação real é responsabilidade do cliente (descartar o token). Revogação server-side de tokens específicos exigiria uma blacklist (ex.: tabela ou Redis com tokens revogados) — não implementado no MVP, considerar se houver necessidade real de "deslogar de todos os dispositivos" no futuro.

## Usuários de desenvolvimento (seed)
Migration `V2__seed_dev_users.sql` cria 3 usuários para testar localmente — **nunca usar em produção**:

| Email | Senha | Papel |
|---|---|---|
| admin@distribuidor.com | Admin123! | admin |
| gerente@distribuidor.com | Gerente123! | gerente_estoque |
| vendedor1@distribuidor.com | Vendedor123! | vendedor |

> Essas senhas (e as mesmas strings usadas em `AuthControllerIntegrationTest.java`) são detectadas pelo scanner do GitGuardian como "Generic Password". É um falso positivo esperado: são credenciais sintéticas, sem vínculo com nenhuma conta ou serviço real, criadas só para dev/teste local. Por isso esses dois arquivos estão na lista de `ignored-paths` em `.gitguardian.yaml`.

## Configuração
`application.yml`:
```yaml
jwt:
  secret: ${JWT_SECRET:__REMOVED__}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}   # 24h
```
Em produção, `JWT_SECRET` **precisa** ser sobrescrito por uma variável de ambiente com um valor forte e secreto — o default só existe para desenvolvimento local funcionar sem configuração extra.

## Teste manual rápido
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@distribuidor.com","password":"Admin123!"}'
```

## Cobertura de testes
- `JwtProviderTest` (unitário): geração/validação/expiração/assinatura inválida
- `AuthServiceTest` (unitário, Mockito): login (sucesso, senha errada, usuário não encontrado), registro (sucesso, email duplicado, role inválida)
- `AuthControllerIntegrationTest` (integração, MockMvc + banco real): todos os status codes do contrato, incluindo controle de acesso por papel (`403` para vendedor tentando registrar usuário)
