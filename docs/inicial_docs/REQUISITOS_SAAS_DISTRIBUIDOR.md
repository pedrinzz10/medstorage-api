# SaaS de Gestão de Pedidos - Distribuidor de Materiais Médicos

## 📋 DOCUMENTO DE REQUISITOS

**Versão:** 1.0  
**Data:** Junho 2026  
**Status:** Em Desenvolvimento (MVP)  
**Prazo:** 4-6 meses  
**Desenvolvedor:** Solo (React + Spring Boot)

---

## 1. VISÃO GERAL DO PROJETO

### 1.1 Problema
A distribuição de materiais médicos para hospitais e clínicas enfrenta desafios em:
- Rastreamento de pedidos em tempo real
- Visibilidade compartilhada entre vendedores
- Integração com controle de estoque
- Medição de performance e comissões dos vendedores
- Notificação de clientes sobre status dos pedidos

### 1.2 Solução
Um SaaS web (desktop inicialmente) que centraliza:
- **Gestão de pedidos** com status rastreável
- **Integração com estoque** (automática)
- **Dashboard de performance** (vendedores)
- **Histórico de clientes** e compras
- **Notificações por email**
- **Gerenciamento de devoluções**

### 1.3 Escopo MVP
- Vendedor cria pedidos manualmente
- Gerente de estoque marca como atendido/retirado
- Estoque decrementar/incrementar automaticamente
- Performance de vendedores (valor, quantidade, comissão)
- Email de notificação quando pedido está pronto
- Suporte a devoluções com reversão de estoque

### 1.4 Fora do Escopo (Futuro)
- Portal web para clientes criarem pedidos próprios
- App mobile
- Integração com nota fiscal eletrônica
- Dashboard de análise avançada
- Relatórios PDF exportáveis

---

## 2. USUÁRIOS E PAPÉIS

### 2.1 Tipos de Usuários

| Papel | Quem | Permissões |
|-------|------|-----------|
| **Vendedor** | Funcionário que faz vendas | Criar pedidos, ver todos os pedidos, ver performance própria |
| **Gerente de Estoque** | Responsável pela separação/embalagem | Marcar pedido como ATENDIDO, marcar como RETIRADO, gerenciar devoluções |
| **Admin/Dono** | Proprietário da empresa | Tudo + gerenciar usuários, tabelas de preço, configurações |
| **Cliente (Futuro)** | Hospital/Clínica | Visualizar próprios pedidos, criar novos pedidos via portal |

### 2.2 Matriz de Permissões

```
                     | Vendedor | Gerente | Admin |
---------------------|----------|---------|-------|
Ver todos pedidos     |    ✓     |    ✓    |   ✓   |
Criar pedido          |    ✓     |    ✗    |   ✓   |
Marcar atendido       |    ✗     |    ✓    |   ✓   |
Marcar retirado       |    ✗     |    ✓    |   ✓   |
Registrar devolução   |    ✗     |    ✓    |   ✓   |
Ver performance       |    ✓*    |    ✗    |   ✓   |
Gerenciar estoque     |    ✗     |    ✓    |   ✓   |
Gerenciar usuários    |    ✗     |    ✗    |   ✓   |
```

*Vendedor vê apenas sua própria performance

---

## 3. USER STORIES E CASOS DE USO

### 3.1 Epic 1: Gestão de Pedidos

#### US-001: Vendedor cria novo pedido
```gherkin
Dado que sou um vendedor logado no SaaS
Quando clico em "Novo Pedido"
Então devo ver um formulário com campos:
  - Seleção de cliente (dropdown com hospitais/clínicas cadastrados)
  - Adição de itens (material médico + quantidade)
  - Preço unitário (autocomplete da tabela, com desconto se aplicável)
  - Tipo de desconto (por quantidade / forma de pagamento)
  - Valor final do pedido (calculado automaticamente)
  - Campo de notas (opcional)
E quando clico "Criar Pedido":
  - O sistema salva com status PENDENTE
  - Aparece no dashboard com data e hora
  - Minha contagem de pedidos sobe
```

**Critérios de Aceitação:**
- ✓ Validar que cliente foi selecionado
- ✓ Validar que pelo menos 1 item foi adicionado
- ✓ Validar quantidade > 0
- ✓ Calcular desconto corretamente
- ✓ Valor final não pode ser negativo
- ✓ Salvar criador do pedido (para rastreamento)

---

#### US-002: Vendedor visualiza lista de pedidos
```gherkin
Dado que sou um vendedor logado
Quando acesso a página "Pedidos"
Então devo ver uma tabela com:
  - ID do pedido
  - Cliente (nome)
  - Data de criação
  - Status (PENDENTE / ATENDIDO / RETIRADO)
  - Vendedor (quem criou)
  - Valor total
  - Ações (editar, deletar, marcar concluído)

E devo poder filtrar por:
  - Status (dropdown)
  - Cliente (search)
  - Vendedor (dropdown)
  - Data (range picker)
  - Intervalo de valor (R$)

E devo poder ordenar por:
  - Data (mais recente primeiro)
  - Valor (maior primeiro)
  - Status
```

**Critérios:**
- ✓ Paginação (20 itens por página)
- ✓ Filters salvos na URL (pra compartilhar)
- ✓ Botão "Resetar filtros"

---

#### US-003: Gerente de estoque marca pedido como ATENDIDO
```gherkin
Dado um pedido com status PENDENTE
Quando o gerente de estoque vê o pedido e clica "Marcar Atendido"
Então:
  - Status muda para ATENDIDO
  - Estoque dos itens decrementar automaticamente
  - Email é enviado pro cliente avisando que está pronto
  - Data/hora é registrada como "data atendimento"
  - Sistema registra quem marcou (para auditoria)

Se houver quantidade insuficiente no estoque:
  - Sistema avisa com warning
  - Opção de permitir mesmo assim (oversell) ou rejeitar
```

**Critérios:**
- ✓ Transação atômica (ou tudo muda, ou nada muda)
- ✓ Log de auditoria
- ✓ Email com detalhes do pedido (número, itens, preço)

---

#### US-004: Gerente de estoque marca pedido como RETIRADO
```gherkin
Dado um pedido com status ATENDIDO
Quando o gerente clica "Marcar Retirado"
Então:
  - Status muda para RETIRADO
  - Pedido é considerado finalizado
  - Contagem de vendas do criador é computada
  - Data/hora é registrada
```

---

### 3.2 Epic 2: Integração com Estoque

#### US-005: Sistema decrementar estoque ao marcar ATENDIDO
```gherkin
Dado um pedido com 50 unidades de "Luva cirúrgica tamanho M"
Quando marca como ATENDIDO
Então:
  - Tabela ESTOQUE recebe UPDATE:
    quantidade = quantidade - 50
  - Se havia 100 unidades, agora tem 50
  - Histórico de movimento registra: tipo=OUT, quantidade=50, motivo="Pedido #123"
```

---

#### US-006: Sistema incrementar estoque ao devolver
```gherkin
Dado um pedido finalizado com 50 unidades de "Luva"
Quando o gerente registra devolução de 10 unidades
Então:
  - Estoque sobe: quantidade = quantidade + 10
  - Novo movimento registrado: tipo=IN, quantidade=10, motivo="Devolução Pedido #123"
  - Status da devolução fica PROCESSADO
```

---

### 3.3 Epic 3: Rastreamento de Clientes

#### US-007: Admin/Vendedor visualiza histórico de cliente
```gherkin
Dado que clico no nome de um cliente em um pedido
Então devo ver:
  - Dados do cliente: nome, email, telefone, CNPJ
  - Endereço
  - Contato principal
  - Histórico de pedidos (últimos 50)
    - Data, ID, itens, valor, status
  - Total gasto (R$) em período selecionável
  - Pedido mais recente
  - Dias desde última compra
```

---

### 3.4 Epic 4: Performance de Vendedores

#### US-008: Vendedor vê sua performance
```gherkin
Dado que sou um vendedor logado
Quando acesso "Minha Performance"
Então devo ver:
  - Total de pedidos criados (este mês)
  - Total vendido (R$) este mês
  - Total de unidades vendidas
  - Comissão a receber (R$)
  - Gráfico: vendas por semana (últimas 4 semanas)
  - Gráfico: top 5 clientes (por valor)
  - Ranking: posição minha vs outros vendedores
```

---

#### US-009: Admin vê performance de todos vendedores
```gherkin
Dado que sou admin
Quando acesso "Performance de Vendedores"
Então devo ver tabela com:
  - Nome do vendedor
  - Total de pedidos (este mês)
  - Valor vendido (R$)
  - Quantidade de unidades
  - Comissão a pagar
  - Taxa de conversão (vendas / tentativas, se aplicável)

Com opções de:
  - Filtrar por período (mês/ano)
  - Ordernar por qualquer coluna
  - Exportar para CSV (futuro)
```

---

### 3.5 Epic 5: Devoluções

#### US-010: Registrar devolução de itens
```gherkin
Dado um pedido finalizado
Quando o gerente clica "Registrar Devolução"
Então:
  - Abre modal para adicionar itens devolvidos
  - Seleciona material e quantidade
  - Pode adicionar motivo (quebrado, não era esperado, etc)
  - Clica "Processar Devolução"
  
E o sistema:
  - Cria registro de DEVOLUÇÃO (status=PROCESSADO)
  - Incrementa estoque automaticamente
  - Registra movimento: tipo=IN (retorno)
  - Calcula reembolso (se aplicável)
```

---

### 3.6 Epic 6: Notificações

#### US-011: Email quando pedido está pronto
```gherkin
Dado um pedido que foi marcado como ATENDIDO
Quando isso acontece
Então email é enviado para:
  - Email do cliente (principal)
  - Email de contato adicional (se houver)

Com conteúdo:
  - Número do pedido
  - Data de criação
  - Itens (com quantidades)
  - Valor total
  - Data de atendimento
  - Instruções para retirada (horário/local)
  - Contato para dúvidas (telefone vendedor)
```

---

## 4. DADOS E MODELOS

### 4.1 Tabelas Principais

```sql
-- USUARIOS
users
  - id (UUID)
  - email (VARCHAR, unique)
  - password_hash (VARCHAR)
  - nome (VARCHAR)
  - role (ENUM: vendedor, gerente_estoque, admin)
  - ativo (BOOLEAN)
  - telefone (VARCHAR, opcional)
  - created_at
  - updated_at

-- CLIENTES
customers
  - id (UUID)
  - nome (VARCHAR)
  - email (VARCHAR)
  - telefone (VARCHAR)
  - cnpj (VARCHAR, opcional)
  - endereco (VARCHAR)
  - contato_principal (VARCHAR)
  - dados_adicionais (JSON - flexível)
  - created_at
  - updated_at

-- TABELA DE PREÇOS
products
  - id (UUID)
  - nome (VARCHAR)
  - descricao (TEXT, opcional)
  - sku (VARCHAR, único)
  - preco_base (DECIMAL 10,2)
  - unidade (VARCHAR: unidade, caixa, etc)
  - estoque_minimo (INT)
  - ativo (BOOLEAN)
  - created_at
  - updated_at

-- ESTOQUE (INTEGRAÇÃO)
inventory
  - id (UUID)
  - product_id (FK)
  - quantidade (INT)
  - data_ultima_atualizacao
  - created_at
  - updated_at

-- MOVIMENTOS DE ESTOQUE (HISTÓRICO)
inventory_movements
  - id (UUID)
  - product_id (FK)
  - tipo (ENUM: IN, OUT)
  - quantidade (INT)
  - motivo (VARCHAR: venda, devolução, ajuste, etc)
  - referencia_id (UUID: pedido, devolução)
  - criado_por (FK users)
  - created_at

-- PEDIDOS
orders
  - id (UUID)
  - numero_pedido (VARCHAR, único, auto-increment friendly)
  - customer_id (FK customers)
  - criado_por (FK users - vendedor)
  - status (ENUM: PENDENTE, ATENDIDO, RETIRADO)
  - valor_total (DECIMAL 10,2)
  - desconto_aplicado (DECIMAL 10,2)
  - tipo_desconto (VARCHAR: quantidade, pagamento, outro)
  - notas (TEXT, opcional)
  - data_atendimento (TIMESTAMP, nullable)
  - data_retirada (TIMESTAMP, nullable)
  - created_at
  - updated_at

-- ITENS DO PEDIDO
order_items
  - id (UUID)
  - order_id (FK orders)
  - product_id (FK products)
  - quantidade (INT)
  - preco_unitario (DECIMAL 10,2)
  - subtotal (DECIMAL 10,2)
  - created_at

-- DEVOLUÇÕES
returns
  - id (UUID)
  - order_id (FK orders)
  - status (ENUM: PENDENTE, PROCESSADO, REJEITADO)
  - data_solicitacao
  - data_processamento (nullable)
  - processado_por (FK users)
  - motivo (TEXT)
  - created_at
  - updated_at

-- ITENS DE DEVOLUÇÃO
return_items
  - id (UUID)
  - return_id (FK returns)
  - product_id (FK products)
  - quantidade (INT)
  - created_at

-- COMISSÕES (CALCULADAS)
commissions
  - id (UUID)
  - vendedor_id (FK users)
  - periodo_inicio (DATE)
  - periodo_fim (DATE)
  - total_pedidos (INT)
  - valor_vendido (DECIMAL 10,2)
  - quantidade_unidades (INT)
  - taxa_comissao (DECIMAL 5,2) -- percentual
  - valor_comissao (DECIMAL 10,2)
  - status (ENUM: PENDENTE, PAGO)
  - created_at
  - updated_at
```

### 4.2 Relacionamentos

```
users (1) ──→ (N) orders (criou)
users (1) ──→ (N) inventory_movements (quem fez)
customers (1) ──→ (N) orders
orders (1) ──→ (N) order_items
products (1) ──→ (N) order_items
products (1) ──→ (1) inventory
products (1) ──→ (N) inventory_movements
orders (1) ──→ (N) returns
returns (1) ──→ (N) return_items
users (1) ──→ (N) commissions
```

---

## 5. FUNCIONALIDADES POR STATUS

### MVP - Fase 1 (Sprints 1-3, ~6 semanas)
- ✅ Autenticação de usuários
- ✅ CRUD de pedidos (criar, listar, editar, deletar)
- ✅ Status de pedidos (PENDENTE → ATENDIDO → RETIRADO)
- ✅ Integração estoque (decrementar ao marcar atendido)
- ✅ Tabela de clientes (criar, editar, visualizar)
- ✅ Email de notificação (quando atendido)
- ✅ Dashboard simples

### Fase 2 (Sprints 4-5, ~4 semanas)
- ✅ Devoluções (registrar, reverter estoque)
- ✅ Performance de vendedores (dashboard)
- ✅ Histórico de cliente (pedidos anteriores)
- ✅ Filtros e buscas avançadas
- ✅ Relatório de comissões

### Fase 3+ (Futuro, pós MVP)
- 🔄 Portal web para clientes criarem pedidos
- 🔄 App mobile (React Native)
- 🔄 Integração NF-e
- 🔄 Análises e gráficos avançados
- 🔄 Integração com sistema contábil

---

## 6. FLUXO DE DADOS

```
VENDEDOR cria Pedido (status=PENDENTE)
    ↓
Pedido salvo no banco com:
  - customer_id
  - criado_por (vendedor)
  - status = PENDENTE
  - items[] com product_id, quantidade, preço
    ↓
GERENTE DE ESTOQUE vê pedido PENDENTE
    ↓
Clica "Marcar Atendido"
    ↓
Sistema:
  1. Atualiza order.status = ATENDIDO
  2. Para cada item do pedido:
     - Decrementa inventory.quantidade
     - Cria inventory_movement (tipo=OUT, motivo=venda)
  3. Envia email para customer.email
  4. Registra data_atendimento
    ↓
Cliente RETIRA o pedido
    ↓
GERENTE clica "Marcar Retirado"
    ↓
Sistema:
  1. Atualiza order.status = RETIRADO
  2. Registra data_retirada
  3. Calcula comissão (se automático)
    ↓
Pedido FINALIZADO ✓
```

---

## 7. REGRAS DE NEGÓCIO

1. **Pedido só pode mudar status uma vez por direção**
   - PENDENTE → ATENDIDO → RETIRADO (não volta)

2. **Estoque não pode ficar negativo**
   - Avisar ao gerente se quantidade é insuficiente
   - Opção: permitir oversell com aviso

3. **Desconto máximo é 50%**
   - Validar no backend

4. **Email deve ser enviado assim que ATENDIDO**
   - Se falhar, registrar tentativa e retry

5. **Comissão calculada sobre valor vendido**
   - Percentual configurável por vendedor ou fixo
   - Só contabiliza pedidos RETIRADOS

6. **Devolução só possível em pedidos RETIRADOS**
   - Reverter para estoque automaticamente

7. **Auditoria: cada ação registra usuário e timestamp**
   - Quem criou, quem atendeu, quem devolveu

---

## 8. CASOS DE ERRO

| Cenário | Ação Esperada |
|---------|--------------|
| Criar pedido sem cliente | Mensagem de erro + form destacada |
| Criar pedido sem itens | Mensagem de erro |
| Quantidade negativa | Não permitir no form |
| Marcar atendido com estoque insuficiente | Aviso + opção confirmar |
| Email falha ao enviar | Log de erro + fila de retry |
| Estoque integrado indisponível | Aviso ao gerente, não bloqueia |
| Usuário sem permissão | Erro 403, redirecionar |
| Pedido não encontrado | Erro 404, mensagem amigável |

---

## 9. SEGURANÇA

- ✓ JWT para autenticação
- ✓ HTTPS em produção
- ✓ Validação em backend (nunca confiar em frontend)
- ✓ Rate limiting em login (max 5 tentativas)
- ✓ Auditoria completa (quem fez o quê, quando)
- ✓ Dados de clientes são privados (RLS se usar Supabase)
- ✓ Senhas com hash + salt (bcrypt)

---

## 10. PERFORMANCE E ESCALABILIDADE

**Para MVP (<50 pedidos/dia):**
- PostgreSQL standard é suficiente
- Cache de produtos em memória (Redis, opcional)
- Paginação de 20 itens por página
- Índices em: customer_id, created_at, status

**Monitoring:**
- Logs centralizados (Sentry ou similar)
- APM básico (resposta de APIs)
- Uptime monitoring

---

## 11. ENDPOINTS DA API (Spring Boot)

### Autenticação
```
POST   /api/auth/login                → token JWT
POST   /api/auth/logout               → invalidar token
POST   /api/auth/refresh              → novo token
POST   /api/auth/register             → criar usuário (admin only)
```

### Pedidos
```
POST   /api/orders                    → criar pedido
GET    /api/orders                    → listar (com filters)
GET    /api/orders/{id}               → detalhe
PUT    /api/orders/{id}               → editar (rascunho)
DELETE /api/orders/{id}               → deletar (status=PENDENTE only)
PATCH  /api/orders/{id}/status        → mudar status
```

### Clientes
```
POST   /api/customers                 → criar cliente
GET    /api/customers                 → listar
GET    /api/customers/{id}            → detalhe + histórico
PUT    /api/customers/{id}            → editar
GET    /api/customers/{id}/orders     → pedidos do cliente
```

### Estoque
```
GET    /api/inventory                 → listar (com estoque)
GET    /api/inventory/{product_id}    → detalhe
POST   /api/inventory/movements       → histórico
```

### Devoluções
```
POST   /api/returns                   → registrar devolução
GET    /api/returns                   → listar devoluções
PATCH  /api/returns/{id}/process      → processar devolução
```

### Performance
```
GET    /api/sellers/performance       → minha performance (vendedor)
GET    /api/sellers/performance/all   → todos (admin)
GET    /api/commissions               → comissões pendentes
```

---

## 12. COMPONENTES REACT (Frontend)

### Páginas principais:
- `LoginPage`
- `DashboardPage` (resumo rápido)
- `OrdersPage` (lista + filtros)
- `OrderCreatePage` (novo pedido)
- `OrderDetailPage` (detalhe + ações)
- `CustomersPage` (lista de clientes)
- `CustomerDetailPage` (histórico)
- `PerformancePage` (vendedor vê sua performance)
- `PerformanceAdminPage` (admin vê todos)
- `ReturnsPage` (gestão de devoluções)
- `SettingsPage` (configurações)

### Componentes reutilizáveis:
- `PaginatedTable`
- `FilterBar`
- `OrderForm`
- `CustomerForm`
- `StatusBadge`
- `ConfirmDialog`
- `NotificationToast`
- `LoadingSpinner`

---

## 13. CRITÉRIOS DE ACEITAÇÃO GERAIS

✓ Toda funcionalidade tem testes unitários (Jest)  
✓ Toda API tem testes de integração (JUnit + Mockito)  
✓ Código segue padrão Clean Code  
✓ Zero console warnings no React  
✓ Responsivo (desktop priority, depois mobile)  
✓ Acessibilidade básica (WCAG 2.1 Level A)  
✓ Documentação de API (Swagger)  

---

**Fim do Documento de Requisitos**

Próximos documentos: 
1. Schema do banco de dados (ERD)
2. Mockups das telas
3. Cronograma detalhado (sprints)
