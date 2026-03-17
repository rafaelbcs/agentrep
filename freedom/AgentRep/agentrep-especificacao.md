# AgentRep — Especificação Revisada (Opção 1 com preparo para B2B)
## “Trust as a Service” para Economias de Agentes (Web3-native)
**Data:** 07 de Março de 2026 | **Última revisão:** 13 de Março de 2026

---

# 1. POSICIONAMENTO (anti-commodity)

## 1.1 O que o AgentRep é
**AgentRep não é um dashboard.** É um *primitivo de confiança* (trust primitive) para agentes:
- Permite agentes **consultarem confiança** de forma autônoma (x402).
- Permite agentes **registrarem outcomes** e evidências.
- Permite **disputa com stake** (consequência econômica) e arbitragem.

Em outras palavras: **“Serviço como Software” = confiança operacional codificada**.

## 1.2 O que o AgentRep não é
- Não é um registry de identidade — essa camada é o ERC-8004.
- Não é “pay-to-win reputation”.
- Não é UI-first.

## 1.3 Diferencial defensável
A defesa não é UI. É **mecanismo econômico + consequência + verificabilidade**:
- **Consulta gratuita** (sem barreira de entrada, foco em adoção).
- **Disputa com stake / slashing** (ataca fraude e incentiva honestidade) — fonte de receita principal.
- **Evidência assinada + hashes + replay** (primeiro passo para anti-hallucination verificável).

> **Diferencial vs MolTrust:** MolTrust faz identidade + credenciais (W3C DID).
> AgentRep faz **outcomes verificáveis + consequência econômica**. São camadas diferentes —
> MolTrust responde “quem é esse agente?”, AgentRep responde “esse agente entrega o que promete?”.

## 1.4 Posicionamento no ecossistema ERC-8004

**ERC-8004** (deployado em 29/01/2026) é um padrão Ethereum que fornece identidade onchain para agentes.
AgentRep **não compete** com ele — **se posiciona como a camada de avaliação e disputa acima dele**.

```
ERC-8004 IdentityRegistry  →  “Quem é esse agente?”          (identidade)
ERC-8004 ReputationRegistry →  “Qual o score básico?”         (armazenamento)
AgentRep                    →  “O que ele fez e foi bom?”     (avaliação + disputa)
```

**Integração estratégica:**
- Agentes registram identidade no **ERC-8004 IdentityRegistry** (não precisam criar conta separada)
- AgentRep usa o endereço EVM como identificador (compatível com ERC-8004 nativamente)
- Após cada score update, AgentRep **escreve o score calculado no ERC-8004 ReputationRegistry**
- Qualquer sistema do ecossistema ERC-8004 (20+ chains) pode ler o score do AgentRep via ERC-8004

**Endereços ERC-8004 (Base Mainnet):**
- `IdentityRegistry:   0x8004A169FB4a3325136EB29fA0ceB6D2e539a432`
- `ReputationRegistry: 0x8004BAa17C55a88189AE136b182e5fdA19dE9b63`

> Na Base Sepolia (testnet), os endereços são configurados como `address(0)` — sync desabilitado.
> Na Base Mainnet (produção), os endereços reais são configurados via `setERC8004Addresses()`.

---

# 2. GO-TO-MARKET ESCOLHIDO

## 2.1 Estratégia
- **Opção 1 primeiro (infra pública):** lançar API + contrato + docs, com foco em comunidade.
- **Preparo para Opção 2 (B2B):** desde o MVP existir “integration kit” para marketplaces.

## 2.2 Canais
- ~~Moltbook~~ — **canal removido:** Meta adquiriu o Moltbook em 10/03/2026; plataforma em processo de encerramento.
- **X** (threads técnicas curtas mostrando dispute + outcomes verificáveis)
- **GitHub** (SDK + exemplos + quickstart)
- **DEV Community / Hashnode** (posts técnicos — comunidade ativa discutindo o problema de trust em agentes)
- **OpenClaw ecosystem** (framework open-source que emergiu do Moltbook, agora na órbita da OpenAI)

---

# 3. ESCOPO: MVP vs V2 vs V3

## 3.1 MVP (4–8 semanas) — “Confiança com consequência econômica”

### Objetivo
Permitir que um agente A contrate/agregue agentes B/C com risco reduzido:
- Consulta reputação (**gratuita** no lançamento)
- Registra outcome (com evidência)
- Se der problema, abre disputa (com stake)

### MVP — Principais Módulos

#### M1) Identidade mínima do agente
- Agente identificado por `walletAddress` (EVM).
- Perfil básico (nome, descrição, categorias).
- Prova de posse da wallet (assinatura de desafio).

#### M2) Reputation Query
- Endpoint **gratuito no lançamento**: `GET /reputation/{address}`.
- Sem barreira de entrada — objetivo é adoção e dados reais.
- Resposta inclui:
  - score global
  - score por categoria
  - taxa de disputa
  - volume transacionado
  - últimos outcomes (limitado)
- **x402 habilitado após tração** (quando houver 50+ agentes registrados com histórico real).

#### M3) Outcome Registry + Evidências
- Registrar outcome com:
  - `taskCategory`
  - `taskDescription`
  - `deliverableUrl` + `deliverableHash`
  - `requesterSignature` (assinatura do agente que solicita/avalia)
  - `valueUsdc`
- Resultado:
  - `SUCCESS` / `FAILURE` / `PARTIAL`
  - motivo curto (LLM Judge + heurísticas)

#### M4) Dispute-lite (com stake)

**Quem pode abrir:** qualquer parte envolvida no outcome (contratante ou contratado).

**Fluxo de estados:**
```
OPEN → EVIDENCE_SUBMITTED → RESOLVED
```

**Etapa 1 — OPEN (abertura)**
- Parte A abre a disputa e deposita $0.50 USDC + paga fee de $0.10 USDC.
- Parte B tem **48h** para também depositar $0.50 USDC.
- Se B não depositar no prazo → B perde automaticamente (sinal de má-fé). Stake de A é devolvido.

**Etapa 2 — EVIDENCE_SUBMITTED (evidências)**
- Cada parte tem **72h** para enviar evidências:
  - O que foi combinado (`taskDescription` original)
  - O que foi entregue (`deliverableUrl` + `deliverableHash`)
  - Argumentação textual
- Tudo registrado com hash (imutável — não pode ser alterado após submissão).

**Etapa 3 — RESOLVED (julgamento)**
- Arbitragem no MVP: **centralizada** (operador / “Judge Agent”) com transparência total.
- Processo de julgamento:
  1. LLM Judge (Claude) analisa as evidências e emite recomendação com reasoning.
  2. Operador revisa e confirma ou substitui o veredicto.
  3. Reasoning completo é publicado e registrado com hash.
- Critérios públicos de julgamento (documentados):
  - A entrega satisfaz o `taskDescription` original?
  - O hash do deliverable bate com o que foi entregue?
  - Há evidência de boa-fé de ambas as partes?

**Distribuição do stake no RESOLVED:**
- **Vencedor:** recebe de volta seu stake + 70% do stake do perdedor.
- **Sistema (AgentRep):** retém 30% do stake do perdedor + a fee de $0.10 (já paga na abertura).
- Score de reputação de ambas as partes é atualizado com base no resultado.

**Slashing de reputação (além do stake):**
- Parte que perde a disputa tem `disputeRate` aumentado no score (penalidade permanente visível).
- Parte que abre disputas frívolas repetidamente (perde >2x seguidas) entra em “probation” — stake dobrado nas próximas disputas.

 **Decisões do MVP (travadas):**
 - Veredictos: **somente** `SUCCESS` / `FAILURE` (sem `PARTIAL`)
 - Stake: **fixo** ($0.50 USDC por parte, total $1.00 por disputa)
 - Árbitro: centralizado no MVP → descentralizado (Jury of Agents) na V2

#### M5) Explorer público (mínimo)
- `/explore/leaderboard`
- `/agent/{address}`

### MVP — O que fica FORA (explicitamente)
- ZK proofs
- Multi-chain
- Token de governança
- Arbitragem DAO completa
- Reputação preditiva

---

## 3.2 V2 (após tração) — “Tribunal de agentes + selos de verificação”

### V2 — Módulos

#### V2.1 Jury of Agents (arbitragem descentralizada incremental)
- Conjunto de “Judge Agents” com reputação alta.
- Seleção pseudo-aleatória ponderada (evita cartel simples).
- **Votação cega** — juízes não se conhecem durante a votação (evita coordenação).

**Modelo de incentivo — distribuição do stake perdido:**
```
Perdedor perde $0.50 de stake. Distribuição:
├── 70% → Vencedor da disputa           ($0.35)
├── 20% → Juízes que votaram com maioria ($0.10 ÷ nº de juízes corretos)
└── 10% → Protocolo AgentRep            ($0.05)
```

> **Princípio:** juízes têm *skin in the game* — precisam fazer stake para
> participar, logo votam pelo mérito e não aleatoriamente. Quem vota com
> a maioria ganha. Quem vota contra perde reputação (e stake na V2 completa).
> Análogo ao incentivo dos mineradores do Bitcoin: participar honestamente
> é mais lucrativo do que atacar a rede.

**Proteção contra cartel:**
- Seleção pseudo-aleatória — não dá prever quem julga cada disputa.
- Punição por inconsistência histórica — juiz que sistematicamente diverge
  da maioria perde stake e é removido do pool.
- Tamanho do júri ímpar (3 ou 5) — evita empate e reduz superfície de ataque.

**Stack técnico (V2):**
- Modelo `Judge` — agente com reputação ≥ threshold configurável.
- `JuryService` — seleciona júri, abre sessão de votação, aplica maioria.
- Smart contract atualizado: `JuryPool`, `stakeJudge()`, `voteDispute()`, `distributeStake()`.
- Fee para juízes pago via x402 por disputa arbitrada.

#### V2.2 Selos pagos (verificação, não boost)
- Selos como **atestado**, nunca como aumento artificial de score.
- Exemplos:
  - `OWNER_KYC_VERIFIED`
  - `CODE_AUDITED`
  - `HIGH_STAKES_APPROVED`
- Modelo:
  - mensalidade ou taxa única

#### V2.3 SDKs
- `agentrep-js`
- `agentrep-python`
- `agentrep-java`

#### V2.4 Webhooks + Bulk APIs (B2B pronto)
- `POST /webhooks` (reputation changed)
- `POST /reputation/bulk` (consulta múltiplos agentes com 1 pagamento)
- `GET /widget/agent/{address}` (HTML/JS embeddable)

---

## 3.3 V3 (R&D) — “Verificabilidade / anti-hallucination”

Antes de ZK, introduzir “verificabilidade incremental”:
- **Replayable traces:** guardar input/output hash + ambiente.
- **Deterministic sandbox:** executar tarefas em sandbox (quando aplicável) e comparar artefatos.
- **Proof-of-Work Logs:** assinatura e hashing de etapas.

ZK entra quando houver caso claro de:
- dado sensível que não pode vazar
- necessidade de prova pública

---

# 4. MODELO ECONÔMICO

## 4.1 Fases de monetização

### Fase 1 — Lançamento (adoção primeiro)
- **Consulta reputação:** GRATUITA
- **Registrar outcome:** GRATUITO
- **Abrir disputa:** stake + fee ← **única fonte de receita no MVP**

### Fase 2 — Após tração (50+ agentes com histórico)
- Liga x402 na consulta de reputação: **$0.001 USDC**
- **Registrar outcome premium (opcional):** $0.002 USDC (evidence pack/armazenamento)

 **Parâmetros MVP (travados):**
 - `QUERY_PRICE_USDC = 0` (gratuito no lançamento)
 - `DISPUTE_STAKE_USDC = 0.50` (por parte)
 - `DISPUTE_FEE_USDC = 0.10` (por disputa)

## 4.2 Incentivos
- Disputa exige stake para reduzir spam.
- Juízes ganham fee.
- Sistema ganha % das taxas.

## 4.3 Anti-abuso
- Anti-replay de pagamentos x402.
- Rate limit por wallet.
- Penalidade para disputas abusivas (perde stake).

---

# 5. DADOS E VERIFICABILIDADE

## 5.1 Evidência mínima (MVP)
- `deliverableHash` (sha256)
- `requesterSignature`
- `timestamp`

## 5.2 Evidência recomendada
- IPFS (para conteúdo público e persistente)
- URL assinado (para conteúdo privado)

---

# 6. INTEGRAÇÃO B2B (preparar desde o começo)

Mesmo sendo Opção 1, o MVP já deve suportar:
- `bulk reputation lookup`
- `webhooks` para “reputation changed”
- `widgets` embeddable
- contratos de SLA futuramente

 **MVP B2B mínimo (para não reescrever depois):**
 - `POST /api/v1/reputation/bulk` (até 100 addresses)
 - `POST /api/v1/webhooks` (register callback)
 - `GET /api/v1/widget/agent/{address}` (snippet embeddable)

---

# 7. CHECKLIST DE LANÇAMENTO (comunidade)

## Antes de lançar
- docs “agent-readable” (markdown curto + exemplos curl)
- um “AgentRep Judge Agent” que usa o produto publicamente

## Lançamento
- thread no X: demo dispute com stake — “agente entregou errado, disputa aberta, stake slashado”
- DEV Community: post técnico “como verificar se um agente AI entrega o que promete”
- GitHub: quickstart + docker compose

---

# 8. PRÓXIMAS DECISÕES (para travar o MVP)

1. **Categorias (MVP): lista fechada inicial**
   - Motivo: melhora ranking, reduz spam e facilita integração B2B.
   - Lista inicial sugerida:
     - `code-review`
     - `data-analysis`
     - `research`
     - `content`
     - `infra`
     - `finance`
     - `trading`
     - `legal`
     - `ops`
2. **O que vai on-chain no MVP: score agregado + eventos**
   - Motivo: custo baixo e leitura simples; mantém “prova pública” via eventos.
   - Outcomes completos ficam no PostgreSQL (mutável) e são referenciados por hashes.

**Decisões já travadas pelo usuário:**
- Sem `PARTIAL`
- Stake fixo
- On-chain apenas agregado + eventos
- Consulta gratuita no lançamento (x402 habilitado após 50+ agentes com histórico)
- Moltbook removido como canal (adquirido pela Meta em 10/03/2026, em encerramento)

---

# 9. ESPECIFICAÇÃO DO CONTRATO (MVP)

## 9.1 O que fica on-chain (AgentRepRegistry.sol)
- **Score agregado por agente** (contadores, success rate, disputeCount)
- **Score por categoria** (categoryHash → CategoryScore)
- **Eventos** para indexação: `OutcomeRecorded`, `DisputeOpened`, `DisputeResolved`, `ScoreUpdated`
- **Sync para ERC-8004 ReputationRegistry** após cada update de score (opcional, configurável)

## 9.2 O que fica off-chain (PostgreSQL)
- Texto de tarefa, evidências completas, reasoning do LLM Judge
- Estado completo de disputas (no MVP)
- Auditoria e rate limiting

## 9.3 Arquitetura de camadas do contrato

```
┌─────────────────────────────────────────┐
│           ERC-8004 (padrão Ethereum)     │
│  IdentityRegistry  │  ReputationRegistry │  ← leitura rápida pelo ecossistema
└────────────────────┴──────────┬──────────┘
                                 │ writeScore() (após cada update)
┌────────────────────────────────▼──────────┐
│           AgentRepRegistry.sol            │
│  registerOutcome()   recordDispute()      │  ← nossa camada (avaliação + disputa)
│  categoryScores      agentScores          │
└───────────────────────────────────────────┘
                     │ eventos
┌────────────────────▼──────────────────────┐
│         Backend (Spring Boot)             │
│  LLM Judge  │  DisputeService  │  x402    │  ← lógica off-chain
└───────────────────────────────────────────┘
```

## 9.4 Interface ERC-8004 (integração)
O contrato define interfaces mínimas para comunicação com ERC-8004:
- `IERC8004IdentityRegistry.isRegistered(address)` — verifica se agente existe
- `IERC8004ReputationRegistry.updateScore(address, uint256, bytes32)` — escreve score

Endereços configuráveis via `setERC8004Addresses()` (owner only).
`address(0)` = sync desabilitado (padrão no testnet).

*Documento criado em 07/03/2026 | Última revisão: 15/03/2026*
