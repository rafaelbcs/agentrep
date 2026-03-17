# AgentRep — Cronograma de Produção
**Atualizado em:** 07 de Março de 2026  
**Stack de IA:** Claude Sonnet 4.6 (MVP, V2 e V3)

---

## STATUS ATUAL

| Projeto | Status | Build |
|---------|--------|-------|
| `backend/` (Java 21 + Spring Boot 3.2) | ✅ Scaffolding completo | `mvn package` → BUILD SUCCESS |
| `frontend/` (React 18 + Vite + Tailwind) | ✅ Scaffolding completo | `npm run build` → OK (2323 modules) |
| `contracts/` (Solidity 0.8.20 + Hardhat) | ✅ Compilando | `hardhat compile` → 5 files OK |
| `docker-compose.yml` + `README.md` | ✅ Criados | — |
| Infra local (Postgres + Redis) | ✅ Configurada | `docker compose up postgres redis -d` |

---

## FASE 0 — Fundação (✅ CONCLUÍDA)
> Duração: Sessões de 07/03/2026  
> Objetivo: estrutura compilando, models, serviços core e contrato deployável

- [x] `pom.xml` com todas as dependências (Spring Boot, Web3j, Spring AI, Flyway, Redis)
- [x] `application.yml` completo (DB, Redis, blockchain, LLM, CORS)
- [x] Domain models: `Agent`, `Outcome`, `Dispute`, `Payment` + todos os enums
- [x] Repositories JPA para todas as entidades
- [x] Flyway migrations: `V1__initial_schema.sql`, `V2__agent_categories.sql`
- [x] `SecurityConfig` + `ApiKeyAuthFilter` (autenticação por API Key)
- [x] `ApiKeyService` (geração SHA-256 + hashing)
- [x] `AgentService` — registro de agentes, geração de API key
- [x] `OutcomeService` — registro de outcomes, avaliação assíncrona (LLM placeholder)
- [x] `ReputationService` — score global + por categoria (com Redis cache)
- [x] `DisputeService` — abrir, resolver disputas, aplicar veredicto
- [x] `GlobalExceptionHandler` — tratamento centralizado de erros
- [x] Controllers: `AgentController`, `ReputationController`, `OutcomeController`, `DisputeController`, `ExplorerController`, `WidgetController`
- [x] DTOs completos (Request/Response para todos os fluxos)
- [x] `AgentRepRegistry.sol` — contrato on-chain com agregados + eventos
- [x] Deploy script Hardhat para Base Sepolia / Mainnet
- [x] Frontend: `LandingPage`, `ExplorerPage`, `AgentProfilePage`, `RegisterPage`
- [x] Frontend: `AgentCard`, `Layout`, `api.ts`, `utils.ts`, types
- [x] `docker-compose.yml` + Dockerfiles (backend + frontend)
- [x] `README.md` + `.env.example`

---

## FASE 1 — MVP Funcional End-to-End
> Meta: **semana de 10/03 a 16/03/2026** (1 semana)  
> Objetivo: sistema rodando localmente com todos os fluxos reais funcionando

### 1.1 Backend — Integrações reais
- [ ] **LLM Judge real** — conectar `OutcomeService.evaluateAsync()` ao Claude Sonnet 4.6 via Spring AI
  - Prompt estruturado: task + deliverable hash + description → SUCCESS / FAILURE + reasoning
  - Confidence score (0.0–1.0)
- [ ] **x402 Payment Filter** — validar tx hash de pagamento USDC real na Base
  - Endpoint `/reputation/{address}` exige `X-Payment-Proof` header
  - Verificar no RPC se a tx existe, foi confirmada e o valor é correto
- [ ] **Web3j integration** — chamar `AgentRepRegistry.registerOutcome()` on-chain após avaliação
- [ ] **WebhookService** — enviar callbacks configurados quando score muda
- [ ] `POST /api/v1/webhooks` — endpoint para registrar callbacks B2B
- [ ] **AgentRegisterRequest.categories** — validação da lista fechada

### 1.2 Contrato — Deploy testnet
- [ ] Deploy `AgentRepRegistry` em Base Sepolia
- [ ] Configurar `CONTRACT_ADDRESS` no `.env`
- [ ] Testar `registerAgent`, `registerOutcome`, `recordDispute` end-to-end
- [ ] Verificar contrato no BaseScan

### 1.3 Frontend — Funcional
- [ ] Conectar todos os endpoints reais (sem mocks)
- [ ] Página de registro com feedback de API key real
- [ ] Explorer com dados reais do backend
- [ ] AgentProfile com radar chart e scores reais
- [ ] Tratamento de erros e estados de loading

### 1.4 Infra dev local
- [ ] `docker compose up --profile full` funcionando
- [ ] Seed script para dados de teste (1 agente, 3 outcomes, 1 dispute)
- [ ] Smoke test curl completo documentado no README

---

## FASE 2 — MVP Hardening + Lançamento público
> Meta: **semana de 17/03 a 23/03/2026** (1 semana)  
> Objetivo: pronto para tráfego real

### 2.1 Segurança e resiliência
- [ ] Rate limiting por wallet (Redis + Bucket4j)
- [ ] Anti-replay de pagamentos x402 (idempotency key na tabela `payments`)
- [ ] Validação de assinatura EVM (`requesterSignature`) no `OutcomeService`
- [ ] Circuit breaker para chamadas ao RPC da Base (Resilience4j)
- [ ] Timeout e retry para chamadas ao Claude Sonnet 4.6

### 2.2 Observabilidade
- [ ] Health check endpoint (`/actuator/health`)
- [ ] Métricas Micrometer (Prometheus) para latências por endpoint
- [ ] Log estruturado (JSON) pronto para ingestão (Grafana / Loki)
- [ ] Alertas para: dispute expirado sem resolução, score sync falhou

### 2.3 Docs agent-readable
- [ ] `docs/api-quickstart.md` — exemplos curl para todos os endpoints
- [ ] `docs/x402-flow.md` — fluxo passo-a-passo de consulta paga
- [ ] `docs/outcome-submission.md` — como registrar um outcome com evidências

### 2.4 Deploy produção (VPS / Railway / Render)
- [ ] Backend + Postgres + Redis em servidor (Railway ou Render)
- [ ] Frontend em Vercel / Netlify
- [ ] DNS configurado (`api.agentrep.xyz`, `agentrep.xyz`)
- [ ] TLS / HTTPS
- [ ] `.env` de produção com chaves reais
- [ ] Deploy contrato em **Base Mainnet**

### 2.5 Lançamento comunidade
- [ ] Post Moltbook: skill `agentrep-query` com x402
- [ ] Thread X: demo de consulta 402 → paga → recebe score
- [ ] Thread X: demo de disputa com stake
- [ ] GitHub: repositório público, README completo, quickstart

---

## FASE 3 — V2: Tribunal de Agentes + SDKs
> Meta: **Abril–Maio 2026** (4–6 semanas com Claude Sonnet 4.6)  
> Trigger: ≥ 50 agentes registrados ou 1 parceiro B2B interessado

### V2.1 Jury of Agents (arbitragem descentralizada)
- [ ] Modelo `Judge` — agente com reputação alta que pode arbitrar
- [ ] Seleção pseudo-aleatória ponderada por score
- [ ] `JuryService` — abrir sessão, coletar votos, aplicar maioria
- [ ] Juízes ganham fee x402 por arbitragem
- [ ] Juízes fazem stake — punição por inconsistência
- [ ] Smart contract atualizado: `JuryPool`, `stakeJudge`, `voteDispute`

### V2.2 Selos de verificação (Badges)
- [ ] Model `AgentBadge` — atestados não infláveis de score
- [ ] Tipos: `OWNER_KYC_VERIFIED`, `CODE_AUDITED`, `HIGH_STAKES_APPROVED`
- [ ] Endpoint `POST /api/v1/badges` (admin/verificador autorizado)
- [ ] Badge visível no AgentProfile e na API de reputação
- [ ] Precificação: mensalidade ou taxa única em USDC

### V2.3 SDKs (Claude Sonnet 4.6 para geração de código)
- [ ] `agentrep-js` — npm package (TypeScript)
  - `AgentRepClient.queryReputation(address)` com x402 integrado
  - `AgentRepClient.submitOutcome(data)` com assinatura EVM
- [ ] `agentrep-python` — PyPI package
  - async/await, compatible com LangChain / CrewAI / AutoGen
- [ ] `agentrep-java` — Maven package
  - Spring Boot autoconfigure

### V2.4 B2B Hardening
- [ ] `WebhookController` completo (CRUD de callbacks)
- [ ] Retry de webhooks com backoff exponencial
- [ ] `POST /reputation/bulk` — batch de até 100 endereços, 1 pagamento
- [ ] Widget JS embeddable (`GET /widget/agent/{address}` retorna snippet HTML/JS)
- [ ] API Keys com escopos (read-only, write, admin)
- [ ] Dashboard B2B (portal self-serve para parceiros)

---

## FASE 4 — V3: Verificabilidade / Anti-Hallucination
> Meta: **Junho–Agosto 2026** (8–12 semanas com Claude Sonnet 4.6)  
> Trigger: V2 estável + demanda de verificação por parceiros enterprise

### V3.1 Replayable Traces
- [ ] Model `ExecutionTrace` — input/output hash + ambiente + timestamps
- [ ] `POST /outcomes/{id}/trace` — submeter trace de execução
- [ ] Hash chain de etapas (assinatura encadeada)
- [ ] Endpoint de replay: `GET /outcomes/{id}/trace/replay`

### V3.2 Deterministic Sandbox
- [ ] Executor sandboxado (Docker-in-Docker ou WASM)
- [ ] Comparação de artefatos: re-executa tarefa e compara hash
- [ ] `SandboxService` — integração com Claude Sonnet 4.6 para re-eval
- [ ] Resultado: `REPRODUCIBLE` / `NON_REPRODUCIBLE` / `INDETERMINATE`

### V3.3 ZK Proofs (quando houver caso claro)
- [ ] Avaliar Noir / Circom para proof de score sem revelar outcomes
- [ ] ZK proof de "agente tem score > X" sem expor histórico
- [ ] Integração com Base (EIP-4844 blobs para dados de prova)

### V3.4 Enterprise
- [ ] SLA contracts (on-chain, paramétrico)
- [ ] Multi-chain (Ethereum L1, Polygon, Arbitrum)
- [ ] Governança DAO (token de votação para parâmetros de protocolo)
- [ ] Auditoria de segurança do contrato (Certik / Trail of Bits)

---

## RESUMO DO CRONOGRAMA

```
Mar/2026  ████████░░░░░░░░░░░░░░░░░░░░░░░░
          [✅ Fase 0]  [Fase 1 ]  [Fase 2 ]

Abr/2026  ░░░░░░░░████████████░░░░░░░░░░░░
                  [     V2 / Fase 3     ]

Mai/2026  ░░░░░░░░████████████████░░░░░░░░
                  [  V2 finalização   ]

Jun/2026  ░░░░░░░░░░░░░░░░████████████████
                              [V3 / Fase 4]

Ago/2026  ████████████████████████████████
          [    V3 + Enterprise + DAO     ]
```

| Fase | Período | Entregável chave |
|------|---------|------------------|
| 0 — Fundação | ✅ 07/03 | Backend + Frontend + Contracts compilando |
| 1 — MVP E2E | 10–16/03 | LLM Judge real + x402 + on-chain sync |
| 2 — MVP Prod | 17–23/03 | Deploy produção + docs + lançamento |
| 3 — V2 | Abr–Mai | Jury + SDKs + B2B hardening |
| 4 — V3 | Jun–Ago | Traces + Sandbox + ZK + Enterprise |

---

## DEPENDÊNCIAS EXTERNAS

| Serviço | Para | Custo estimado |
|---------|------|----------------|
| Anthropic API (Claude Sonnet 4.6) | LLM Judge (MVP→V3) | ~$3–15/1k disputes |
| Alchemy / QuickNode (Base) | RPC para on-chain | ~$49/mês starter |
| Railway / Render | Hosting backend | ~$20–50/mês |
| Vercel / Netlify | Hosting frontend | Gratuito |
| Base Sepolia faucet | Testes testnet | Gratuito |
| Base Mainnet gas | Deploy + writes | ~$5–20 total MVP |

---

## MÉTRICAS DE SUCESSO POR FASE

| Fase | Métrica | Alvo |
|------|---------|------|
| MVP Lançamento | Agentes registrados | ≥ 10 |
| MVP + 2 semanas | Outcomes registrados | ≥ 50 |
| MVP + 1 mês | Consultas x402 pagas | ≥ 100 |
| V2 | Parceiros B2B | ≥ 1 |
| V2 | Agentes com score | ≥ 100 |
| V3 | Traces verificados | ≥ 500 |

---

*Cronograma criado em 07/03/2026 — revisão semanal toda segunda-feira*
