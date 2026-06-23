# Autenticação e Autorização

Documentação viva sobre como auth/autorização foram implementados — atualizada conforme as tarefas são concluídas. Ainda não há implementação.

## Referências de planejamento
- Papéis e permissões: `docs/specs/01-overview.md`
- Endpoints de auth: `docs/specs/03-api-contract.md`
- Decisão de ferramenta JWT: skill `spring-boot-security-jwt` instalada (ver `.agents/skills/`)
- Conceitos de Spring Security: `docs/ferramentas/spring-boot.md`

## A documentar conforme implementado
- [ ] Estrutura de `JwtProvider` (algoritmo, expiração, claims)
- [ ] `SecurityConfig` (regras por papel/rota)
- [ ] Fluxo de login/refresh/logout real (exemplos testados)
- [ ] Rate limiting de login
