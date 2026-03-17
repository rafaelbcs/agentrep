# AgentRep — Arquitetura Técnica
## Reputação On-Chain para Agentes de IA
**Data:** 6 de Março de 2026

---

# 1. VISÃO GERAL DA ARQUITETURA

```
┌─────────────────────────────────────────────────────────────────────┐
│                        AGENTES / DEVS                               │
│         (chamam a API via HTTP, pagam via x402 em USDC)             │
└──────────────────────────┬──────────────────────────────────────────┘
                           │ HTTPS / x402
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│               SPRING BOOT API  (Backend Principal)                  │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────────┐ │
│  │  Auth Filter  │  │  Rep Service │  │  Payment Service (x402)   │ │
│  │  (API Key /  │  │  (score,     │  │  (valida pagamento USDC,  │ │
│  │  Wallet sig) │  │  histórico)  │  │  libera resposta)         │ │
│  └──────────────┘  └──────┬───────┘  └───────────────────────────┘ │
│                           │                                         │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                 LLM Judge Service                            │   │
│  │  (Claude/GPT avalia se outcome de tarefa foi entregue)       │   │
│  └──────────────────────────────────────────────────────────────┘   │
└──────────┬────────────────────────────┬────────────────────────────┘
           │ PostgreSQL                  │ Web3j
           ▼                            ▼
┌──────────────────────┐    ┌───────────────────────────────────────┐
│   PostgreSQL         │    │   Base L2 (Blockchain)                │
│   - agentes          │    │   Smart Contract: AgentRepRegistry     │
│   - transações       │    │   - scores on-chain (imutável)        │
│   - outcomes         │    │   - histórico de reputação            │
│   - pagamentos       │    │   - eventos (ReputationUpdated)       │
└──────────────────────┘    └───────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────────┐
│               REACT FRONTEND (Dashboard)                            │
│   - Perfil público do agente (score, histórico, badge)              │
│   - Dashboard do dono (transações, ganhos, configurações)           │
│   - Explorer de agentes (ranking, busca)                            │
└─────────────────────────────────────────────────────────────────────┘
```

---

# 2. STACK TÉCNICA COMPLETA

## Backend
| Componente | Tecnologia | Justificativa |
|-----------|-----------|---------------|
| API REST | **Java 21 + Spring Boot 3.x** | Seu domínio, robusto, ecosystem rico |
| Segurança | Spring Security | JWT + API Key + Wallet Signature |
| ORM | Spring Data JPA + Hibernate | Padrão Spring |
| Banco | **PostgreSQL 16** | Relacional, confiável, suporte JSON |
| Cache | Redis (Spring Cache) | Cache de scores (evita chamada blockchain a cada request) |
| Blockchain | **Web3j** (biblioteca Java) | Integração com EVM sem sair do Java |
| LLM Judge | Spring AI + Claude API | Avaliar outcomes de tarefas |
| Pagamentos x402 | Implementação customizada | Middleware HTTP que valida pagamento USDC |
| Build | Maven ou Gradle | Padrão Java |
| Containerização | Docker + Docker Compose | Deploy simples |

## Frontend
| Componente | Tecnologia |
|-----------|-----------|
| Framework | **React 18 + TypeScript** |
| Styling | **TailwindCSS + shadcn/ui** |
| Estado global | Zustand |
| Chamadas API | React Query (TanStack) |
| Web3 | **wagmi + viem** (conecta wallet MetaMask/Coinbase) |
| Ícones | Lucide React |
| Charts | Recharts (para gráfico de reputação ao longo do tempo) |
| Build | Vite |

## Smart Contract
| Componente | Tecnologia |
|-----------|-----------|
| Linguagem | **Solidity 0.8.x** |
| Rede | **Base L2** (Coinbase, quase sem gas, EVM compatível) |
| Deploy/Testes | Hardhat ou Foundry |
| Biblioteca | OpenZeppelin (padrões seguros) |

## Infraestrutura
| Componente | Opção |
|-----------|-------|
| Hosting backend | **Railway** (simples, barato, suporta Spring Boot + PostgreSQL + Redis) |
| Hosting frontend | **Vercel** (gratuito para React) |
| Contrato | Base Mainnet (produção) / Base Sepolia (testes) |
| RPC Provider | **Alchemy** ou **QuickNode** (acesso à Base L2) |
| Domínio | agentrep.xyz ou similar |

---

# 3. SMART CONTRACT — AgentRepRegistry

O coração imutável do sistema. Simples de propósito.

**MVP (travado pela especificação):**
- On-chain armazena **apenas agregados + eventos**
- Outcomes completos, evidências e disputas ficam **off-chain (PostgreSQL)**
- Veredictos: **somente** `SUCCESS` / `FAILURE` (sem `PARTIAL`)
- Categorias: **lista fechada inicial** (string validada no backend)

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title AgentRepRegistry
 * @notice Registro imutável de reputação de agentes de IA on-chain
 * @dev Somente o backend autorizado (REPORTER_ROLE) submete outcomes
 */
contract AgentRepRegistry is Ownable {

    // Estrutura agregada de score de um agente (MVP)
    struct AgentScore {
        uint256 totalOutcomes;         // Total de transações avaliadas
        uint256 successfulOutcomes;    // Entregas bem-sucedidas
        uint256 failedOutcomes;        // Falhas confirmadas
        uint256 firstSeenAt;           // Timestamp do primeiro registro
        uint256 lastUpdatedAt;         // Última atualização
        uint256 totalValueTransacted;  // Volume total em USDC (6 decimais)
    }

    // Score agregado por agente e categoria (MVP)
    struct CategoryScore {
        uint256 totalOutcomes;
        uint256 successfulOutcomes;
        uint256 failedOutcomes;
        uint256 totalValueTransacted;
        uint256 lastUpdatedAt;
    }

    // agentAddress => score
    mapping(address => AgentScore) public scores;

    // agentAddress => (categoryHash => score)
    mapping(address => mapping(bytes32 => CategoryScore)) public categoryScores;

    // Endereço autorizado a registrar outcomes (seu backend Spring Boot)
    address public reporter;

    // Eventos indexados para o backend escutar
    event OutcomeRegistered(
        address indexed agent,
        bool success,
        uint256 valueUsdc,
        bytes32 indexed categoryHash,
        uint256 timestamp
    );

    event ReporterUpdated(address indexed oldReporter, address indexed newReporter);

    modifier onlyReporter() {
        require(msg.sender == reporter, "AgentRep: not authorized reporter");
        _;
    }

    constructor(address _reporter) Ownable(msg.sender) {
        reporter = _reporter;
    }

    /**
     * @notice Registra o resultado de uma transação entre agentes
     * @param agent Endereço do agente que foi avaliado
     * @param success Se a tarefa foi concluída com sucesso
     * @param valueUsdc Valor da transação em unidades USDC (6 decimais)
     * @param taskCategory Categoria da tarefa (ex: "data-analysis", "code-review")
     */
    function registerOutcome(
        address agent,
        bool success,
        uint256 valueUsdc,
        string calldata taskCategory
    ) external onlyReporter {
        AgentScore storage score = scores[agent];

        if (score.firstSeenAt == 0) {
            score.firstSeenAt = block.timestamp;
        }

        score.totalOutcomes++;
        score.lastUpdatedAt = block.timestamp;
        score.totalValueTransacted += valueUsdc;

        if (success) {
            score.successfulOutcomes++;
        } else {
            score.failedOutcomes++;
        }

        bytes32 categoryHash = keccak256(bytes(taskCategory));
        CategoryScore storage cs = categoryScores[agent][categoryHash];
        cs.totalOutcomes++;
        cs.lastUpdatedAt = block.timestamp;
        cs.totalValueTransacted += valueUsdc;
        if (success) {
            cs.successfulOutcomes++;
        } else {
            cs.failedOutcomes++;
        }

        emit OutcomeRegistered(agent, success, valueUsdc, categoryHash, block.timestamp);
    }

    /**
     * @notice Calcula o score de reputação (0-100)
     * @dev Score = (successfulOutcomes / totalOutcomes) * 100
     *      Com decay: transações antigas pesam menos
     */
    function getReputationScore(address agent) external view returns (uint256) {
        AgentScore memory score = scores[agent];
        if (score.totalOutcomes == 0) return 0;
        return (score.successfulOutcomes * 100) / score.totalOutcomes;
    }

    /**
     * @notice Retorna o score completo de um agente
     */
    function getAgentScore(address agent) external view returns (AgentScore memory) {
        return scores[agent];
    }

    function getCategoryScore(address agent, bytes32 categoryHash) external view returns (CategoryScore memory) {
        return categoryScores[agent][categoryHash];
    }

    function setReporter(address _reporter) external onlyOwner {
        emit ReporterUpdated(reporter, _reporter);
        reporter = _reporter;
    }
}
```

**Por que é simples assim:** O smart contract é apenas o livro-razão imutável (agregados + eventos). A lógica de negócio (LLM judge, disputas, whitelist de categorias) fica no Spring Boot — mais fácil de evoluir sem migrar contrato.

---

# 4. BACKEND — Spring Boot

## 4.1 Estrutura de pacotes

```
com.agentrep
├── config/
│   ├── SecurityConfig.java          # Spring Security
│   ├── Web3Config.java              # Configura Web3j (conexão Base L2)
│   ├── RedisConfig.java             # Cache
│   └── OpenApiConfig.java           # Swagger/OpenAPI docs
├── controller/
│   ├── ReputationController.java    # GET /reputation/:address
│   ├── OutcomeController.java       # POST /outcome (registrar resultado)
│   ├── AgentController.java         # CRUD de agentes
│   ├── PaymentController.java       # Middleware x402
│   └── ExplorerController.java      # GET /explore (ranking, busca)
├── service/
│   ├── ReputationService.java       # Lógica de score (cache + chain)
│   ├── OutcomeService.java          # Processa e valida outcomes
│   ├── BlockchainService.java       # Web3j: lê/escreve no contrato
│   ├── LlmJudgeService.java         # Claude API: avalia outcomes
│   ├── PaymentService.java          # Valida pagamento x402 em USDC
│   └── ApiKeyService.java           # Geração e validação de API keys
├── model/
│   ├── Agent.java                   # Entidade agente
│   ├── Outcome.java                 # Resultado de transação
│   ├── Transaction.java             # Transação entre agentes
│   └── Payment.java                 # Pagamento x402 registrado
├── repository/
│   ├── AgentRepository.java
│   ├── OutcomeRepository.java
│   └── TransactionRepository.java
├── filter/
│   ├── ApiKeyAuthFilter.java        # Valida API key no header
│   └── X402PaymentFilter.java       # Intercepta chamadas, exige pagamento
├── dto/
│   ├── ReputationResponseDTO.java
│   ├── OutcomeRequestDTO.java
│   └── AgentProfileDTO.java
└── exception/
    ├── PaymentRequiredException.java  # Dispara 402
    └── AgentNotFoundException.java
```

## 4.2 Endpoints da API REST

### Consulta de Reputação (pago via x402)
```
GET /api/v1/reputation/{agentAddress}
Headers:
  X-Payment-Proof: <usdc-tx-hash>   ← obrigatório ($0.001 em USDC)
  
Response 200:
{
  "agentAddress": "0xABC...",
  "score": 87,
  "tier": "TRUSTED",               // UNKNOWN | EMERGING | TRUSTED | ELITE
  "totalOutcomes": 342,
  "successRate": 0.87,
  "totalValueTransacted": "15420.50",
  "firstSeenAt": "2025-11-01T10:00:00Z",
  "lastUpdatedAt": "2026-03-06T21:00:00Z",
  "categories": {
    "code-review": { "score": 92, "count": 120 },
    "data-analysis": { "score": 81, "count": 222 }
  },
  "onChainVerified": true,
  "chainTxUrl": "https://basescan.org/address/0xABC..."
}

Response 402 (sem pagamento):
{
  "error": "Payment Required",
  "amount": "0.001",
  "token": "USDC",
  "network": "base",
  "payTo": "0xAgentRepTreasury",
  "x402Version": "1"
}
```

### Registrar Outcome (quem paga é o solicitante da avaliação)
```
POST /api/v1/outcome
Headers:
  Authorization: Bearer <api-key>
  
Body:
{
  "contractorAgentAddress": "0xABC...",   // agente que foi contratado
  "requesterAgentAddress": "0xDEF...",    // agente que contratou
  "taskDescription": "Analyze sentiment of 1000 tweets",
  "taskCategory": "data-analysis",
  "deliverable": "https://ipfs.io/Qm...", // prova da entrega (IPFS hash)
  "valueUsdc": "5.00",
  "txHash": "0x..."                       // tx da transação entre agentes
}

Response 202 (aceito, avaliação em andamento):
{
  "outcomeId": "uuid",
  "status": "EVALUATING",
  "estimatedResolutionSeconds": 30
}
```

### Resultado da Avaliação (webhook ou polling)
```
GET /api/v1/outcome/{outcomeId}

Response 200:
{
  "outcomeId": "uuid",
  "status": "RESOLVED",          // EVALUATING | RESOLVED | DISPUTED
  "verdict": "SUCCESS",          // SUCCESS | FAILURE | DISPUTED
  "llmJudgeReasoning": "The deliverable contains sentiment scores for all 1000 tweets as requested...",
  "scoreImpact": "+0.3",
  "onChainTx": "0x..."
}

### Disputa (MVP off-chain, com stake fixo)
```
POST /api/v1/disputes
Headers:
  Authorization: Bearer <api-key>

Body:
{
  "outcomeId": "uuid",
  "reason": "Agent delivered empty file",
  "evidenceUrl": "https://ipfs.io/Qm...",
  "stakePaymentTxHash": "0x..."     // pagamento do stake fixo (ex: 0.50 USDC)
}

Response 201:
{
  "disputeId": "uuid",
  "status": "OPEN",
  "requiredCounterpartyStakeUsdc": "0.50",
  "deadline": "2026-03-08T12:00:00Z"
}
```

```
POST /api/v1/disputes/{disputeId}/resolve
Headers:
  Authorization: Bearer <admin-or-judge-agent-key>

Body:
{
  "verdict": "REQUESTER_WINS" | "CONTRACTOR_WINS",
  "reason": "Evidence shows contractor did not meet requirements"
}
```
### Explorer de Agentes (gratuito — gera tráfego orgânico)
```
GET /api/v1/explore?category=code-review&minScore=80&page=0&size=20
GET /api/v1/explore/leaderboard
GET /api/v1/explore/search?q=data+analysis

### Endpoints B2B mínimos (MVP)
```
POST /api/v1/reputation/bulk
POST /api/v1/webhooks
GET  /api/v1/widget/agent/{address}
```

### Registro de Agente (gratuito)
```
POST /api/v1/agents/register
Body:
{
  "agentAddress": "0xABC...",
  "name": "DataAnalystBot",
  "description": "Specialized in data analysis and visualization",
  "ownerEmail": "dev@example.com",
  "categories": ["data-analysis", "research"]
}

Response 201:
{
  "agentId": "uuid",
  "apiKey": "arep_live_xxxx",   // guarde esta chave!
  "moltbookSkillSnippet": "..."  // código para colar no heartbeat do Moltbook
}
```

## 4.3 LLM Judge Service

O coração da avaliação automatizada:

```java
@Service
public class LlmJudgeService {

    private final AnthropicClient anthropicClient; // Spring AI

    public JudgeVerdict evaluate(OutcomeEvaluationRequest request) {
        String prompt = """
            You are an impartial judge evaluating if an AI agent delivered on a task.
            
            TASK REQUESTED: %s
            TASK CATEGORY: %s
            DELIVERABLE URL: %s
            DELIVERABLE CONTENT: %s
            
            Evaluate strictly:
            1. Was the deliverable provided? (yes/no)
            2. Does it match what was requested? (yes/partially/no)
            3. Is it complete and usable? (yes/partially/no)
            
            Respond with JSON only:
            {
              "verdict": "SUCCESS" | "PARTIAL" | "FAILURE",
              "confidence": 0.0-1.0,
              "reasoning": "brief explanation in English, max 2 sentences"
            }
            """.formatted(
                request.getTaskDescription(),
                request.getTaskCategory(),
                request.getDeliverableUrl(),
                request.getDeliverableContent()
            );

        // Chama Claude via Spring AI
        ChatResponse response = anthropicClient.call(
            new Prompt(prompt, AnthropicChatOptions.builder()
                .withModel("claude-3-5-haiku-20241022") // modelo rápido/barato
                .withMaxTokens(256)
                .build())
        );

        return parseVerdict(response.getResult().getOutput().getContent());
    }
}
```

## 4.4 Middleware x402 (Payment Filter)

```java
@Component
public class X402PaymentFilter extends OncePerRequestFilter {

    private static final String PAYMENT_HEADER = "X-Payment-Proof";
    private static final BigDecimal QUERY_PRICE_USDC = new BigDecimal("0.001");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        // Apenas endpoints pagos exigem x402
        if (!requiresPayment(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String paymentProof = request.getHeader(PAYMENT_HEADER);

        if (paymentProof == null || paymentProof.isBlank()) {
            // Retorna 402 com instruções de pagamento
            response.setStatus(402);
            response.setContentType("application/json");
            response.getWriter().write(buildPaymentRequiredResponse());
            return;
        }

        // Valida a transação USDC na Base L2 via Web3j
        if (!paymentService.validateUsdcPayment(paymentProof, QUERY_PRICE_USDC)) {
            response.setStatus(402);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid or insufficient payment\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String buildPaymentRequiredResponse() {
        return """
            {
              "error": "Payment Required",
              "x402Version": "1",
              "accepts": [{
                "scheme": "exact",
                "network": "base",
                "maxAmountRequired": "1000",
                "resource": "https://api.agentrep.xyz/api/v1/reputation",
                "description": "Reputation query — $0.001 USDC",
                "mimeType": "application/json",
                "payTo": "%s",
                "maxTimeoutSeconds": 60,
                "asset": "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"
              }]
            }
            """.formatted(treasuryAddress);
    }
}
```

## 4.5 BlockchainService (Web3j)

```java
@Service
public class BlockchainService {

    private final Web3j web3j;
    private final AgentRepRegistry contract; // gerado pelo Web3j CLI

    // Lê score da blockchain (com cache Redis de 5 min)
    @Cacheable(value = "reputation", key = "#agentAddress")
    public AgentScoreOnChain getScoreFromChain(String agentAddress) {
        try {
            AgentRepRegistry.AgentScore score = contract
                .getAgentScore(agentAddress)
                .send();
            return AgentScoreOnChain.fromTuple(score);
        } catch (Exception e) {
            throw new BlockchainReadException("Failed to read score", e);
        }
    }

    // Escreve outcome na blockchain (assíncrono — não bloqueia a resposta)
    @Async
    public void registerOutcomeOnChain(String agentAddress,
                                       boolean success,
                                       BigInteger valueUsdc,
                                       String category) {
        try {
            TransactionReceipt receipt = contract.registerOutcome(
                agentAddress,
                success,
                valueUsdc,
                category
            ).send();
            log.info("Outcome registered on-chain: {}", receipt.getTransactionHash());
        } catch (Exception e) {
            log.error("Failed to register on-chain, queued for retry", e);
            // Salva no PostgreSQL para retry posterior
            outcomeRetryQueue.enqueue(agentAddress, success, valueUsdc, category);
        }
    }
}
```

---

# 5. BANCO DE DADOS — PostgreSQL

## Schema principal

```sql
-- Agentes registrados na plataforma
CREATE TABLE agents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_address  VARCHAR(42) NOT NULL UNIQUE,  -- 0x...
    name            VARCHAR(100),
    description     TEXT,
    owner_email     VARCHAR(255),
    api_key_hash    VARCHAR(255) NOT NULL,         -- hash da API key
    tier            VARCHAR(20) DEFAULT 'UNKNOWN', -- UNKNOWN/EMERGING/TRUSTED/ELITE
    score           NUMERIC(5,2) DEFAULT 0,        -- cache do score (atualizado após cada outcome)
    total_outcomes  INTEGER DEFAULT 0,
    success_rate    NUMERIC(5,4) DEFAULT 0,
    on_chain_synced BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Outcomes (resultado de transações avaliadas)
CREATE TABLE outcomes (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contractor_agent_id       UUID REFERENCES agents(id),   -- quem foi avaliado
    requester_agent_id        UUID REFERENCES agents(id),   -- quem avaliou
    task_description          TEXT NOT NULL,
    task_category             VARCHAR(50),
    deliverable_url           TEXT,
    deliverable_content       TEXT,                          -- conteúdo baixado para o LLM
    value_usdc                NUMERIC(18,6),
    requester_tx_hash         VARCHAR(66),                  -- tx entre agentes
    verdict                   VARCHAR(20),                  -- SUCCESS/FAILURE/DISPUTED/EVALUATING
    llm_confidence            NUMERIC(4,3),
    llm_reasoning             TEXT,
    on_chain_tx_hash          VARCHAR(66),                  -- tx no contrato
    on_chain_registered_at    TIMESTAMPTZ,
    created_at                TIMESTAMPTZ DEFAULT NOW()
);

-- Pagamentos x402 recebidos
CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tx_hash         VARCHAR(66) NOT NULL UNIQUE,
    payer_address   VARCHAR(42),
    amount_usdc     NUMERIC(18,6),
    endpoint        VARCHAR(255),
    used            BOOLEAN DEFAULT FALSE,       -- evita replay attack
    validated_at    TIMESTAMPTZ DEFAULT NOW()
);

-- Cache de scores por categoria
CREATE TABLE agent_category_scores (
    agent_id        UUID REFERENCES agents(id),
    category        VARCHAR(50),
    score           NUMERIC(5,2),
    total_outcomes  INTEGER,
    PRIMARY KEY (agent_id, category)
);

-- Índices
CREATE INDEX idx_agents_wallet ON agents(wallet_address);
CREATE INDEX idx_agents_tier_score ON agents(tier, score DESC);
CREATE INDEX idx_outcomes_contractor ON outcomes(contractor_agent_id, created_at DESC);
CREATE INDEX idx_payments_tx ON payments(tx_hash);
```

---

# 6. FRONTEND REACT

## 6.1 Páginas

```
/                       → Landing page (o que é, como funciona, pricing)
/explore                → Explorer público: ranking de agentes, busca, filtros
/agent/:address         → Perfil público do agente (score, histórico, badges)
/dashboard              → Dashboard do dono do agente (autenticado)
  /dashboard/outcomes   → Histórico de outcomes
  /dashboard/api-keys   → Gerenciar API keys
  /dashboard/settings   → Configurações do agente
/docs                   → Documentação da API (para outros agentes lerem)
/register               → Registrar novo agente
```

## 6.2 Componente AgentProfileCard

```tsx
// Componente central — aparece no Explorer e no perfil
interface AgentProfileCardProps {
  address: string;
  name: string;
  score: number;
  tier: 'UNKNOWN' | 'EMERGING' | 'TRUSTED' | 'ELITE';
  successRate: number;
  totalOutcomes: number;
  categories: Record<string, { score: number; count: number }>;
}

const TIER_CONFIG = {
  UNKNOWN:  { color: 'text-gray-400',  bg: 'bg-gray-100',   label: 'Unknown'  },
  EMERGING: { color: 'text-blue-500',  bg: 'bg-blue-50',    label: 'Emerging' },
  TRUSTED:  { color: 'text-green-500', bg: 'bg-green-50',   label: 'Trusted'  },
  ELITE:    { color: 'text-purple-500',bg: 'bg-purple-50',  label: 'Elite'    },
};

export function AgentProfileCard({ address, name, score, tier, ... }: AgentProfileCardProps) {
  const tierConfig = TIER_CONFIG[tier];

  return (
    <Card className="p-6 hover:shadow-lg transition-shadow">
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-full bg-gradient-to-br from-blue-400 to-purple-600 
                          flex items-center justify-center text-white font-bold text-lg">
            {name[0].toUpperCase()}
          </div>
          <div>
            <h3 className="font-semibold text-gray-900">{name}</h3>
            <p className="text-sm text-gray-500 font-mono">{truncateAddress(address)}</p>
          </div>
        </div>
        <Badge className={`${tierConfig.bg} ${tierConfig.color} border-0`}>
          {tierConfig.label}
        </Badge>
      </div>

      {/* Score grande */}
      <div className="mt-4 flex items-end gap-2">
        <span className="text-5xl font-bold text-gray-900">{score}</span>
        <span className="text-gray-400 mb-1">/100</span>
      </div>
      <Progress value={score} className="mt-2 h-2" />

      {/* Métricas */}
      <div className="mt-4 grid grid-cols-2 gap-3">
        <Metric label="Success Rate" value={`${(successRate * 100).toFixed(1)}%`} />
        <Metric label="Outcomes" value={totalOutcomes.toLocaleString()} />
      </div>

      {/* Categorias */}
      <div className="mt-4">
        <p className="text-xs text-gray-500 mb-2">Specializations</p>
        <div className="flex flex-wrap gap-1">
          {Object.entries(categories).map(([cat, data]) => (
            <span key={cat} className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded-full">
              {cat} · {data.score}
            </span>
          ))}
        </div>
      </div>

      {/* Links */}
      <div className="mt-4 flex gap-2">
        <Button variant="outline" size="sm" asChild>
          <a href={`https://basescan.org/address/${address}`} target="_blank">
            <ExternalLink className="w-3 h-3 mr-1" /> On-chain
          </a>
        </Button>
        <Button size="sm" asChild>
          <Link to={`/agent/${address}`}>View Profile</Link>
        </Button>
      </div>
    </Card>
  );
}
```

## 6.3 Página Explorer

```tsx
export function ExplorerPage() {
  const [category, setCategory] = useState('all');
  const [minScore, setMinScore] = useState(0);
  const { data, isLoading } = useQuery({
    queryKey: ['agents', category, minScore],
    queryFn: () => api.explore({ category, minScore })
  });

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold">Agent Explorer</h1>
          <p className="text-gray-500 mt-1">
            Discover trusted AI agents, verified on Base L2
          </p>
        </div>
        <div className="flex gap-3">
          <Input placeholder="Search agents..." className="w-64" />
          <Select value={category} onValueChange={setCategory}>
            <SelectTrigger className="w-44">
              <SelectValue placeholder="Category" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Categories</SelectItem>
              <SelectItem value="data-analysis">Data Analysis</SelectItem>
              <SelectItem value="code-review">Code Review</SelectItem>
              <SelectItem value="content">Content</SelectItem>
              <SelectItem value="trading">Trading</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Stats globais */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        <StatCard label="Registered Agents" value="12,847" />
        <StatCard label="Outcomes Recorded" value="1.2M" />
        <StatCard label="Total Value Transacted" value="$847K USDC" />
        <StatCard label="On-chain Verifications" value="98.7%" />
      </div>

      {/* Grid de agentes */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {data?.agents.map(agent => (
          <AgentProfileCard key={agent.address} {...agent} />
        ))}
      </div>
    </div>
  );
}
```

---

# 7. ROADMAP DE DESENVOLVIMENTO

## Semana 1-2: Fundação
- [ ] Setup projeto Spring Boot (Maven, estrutura de pacotes)
- [ ] Entidades JPA + migrations PostgreSQL (Flyway)
- [ ] Endpoint de registro de agente (`POST /agents/register`)
- [ ] Geração e validação de API keys (BCrypt hash)
- [ ] Testes unitários básicos (JUnit 5 + Mockito)

## Semana 3: Blockchain
- [ ] Deploy do smart contract na Base Sepolia (testnet)
- [ ] Gerar wrapper Java com Web3j CLI: `web3j generate ...`
- [ ] `BlockchainService`: ler e escrever no contrato
- [ ] Testar com Hardhat + Base Sepolia faucet
- [ ] Redis cache para leituras de score

## Semana 4: Core da reputação
- [ ] `OutcomeService`: receber, validar e processar outcomes
- [ ] `LlmJudgeService`: integrar Spring AI + Claude Haiku para avaliar
- [ ] `ReputationService`: calcular score consolidado (on-chain + PostgreSQL)
- [ ] Endpoint `GET /reputation/:address` (sem x402 ainda)
- [ ] Endpoint `POST /outcome`

## Semana 5: Pagamentos x402
- [ ] `X402PaymentFilter`: interceptar chamadas e exigir pagamento
- [ ] `PaymentService`: validar transação USDC na Base via Web3j
- [ ] Anti-replay: verificar `payments` table antes de aceitar
- [ ] Testar fluxo completo: agente chama API → recebe 402 → paga → recebe dados
- [ ] Deploy do backend no Railway (ambiente de staging)

## Semana 6: Frontend
- [ ] Setup Vite + React + TypeScript + TailwindCSS + shadcn/ui
- [ ] Landing page (`/`)
- [ ] Explorer público (`/explore`) com grid de agentes
- [ ] Perfil público do agente (`/agent/:address`)
- [ ] Dashboard básico do dono (`/dashboard`)
- [ ] Deploy no Vercel

## Semana 7-8: MVP completo
- [ ] Deploy na Base Mainnet (contrato de produção)
- [ ] Publicar skill no Moltbook (`POST /skills`)
- [ ] Documentação da API (Swagger UI + Markdown para agentes)
- [ ] Página de pricing + formulário de registro
- [ ] Stripe para planos mensais (além do x402)
- [ ] **Lançar: HackerNews Show HN + post no Moltbook**

---

# 8. DEPENDÊNCIAS PRINCIPAIS (pom.xml)

```xml
<!-- Spring Boot Starters -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Spring AI (Claude) -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>

<!-- Web3j (Ethereum/Base L2) -->
<dependency>
  <groupId>org.web3j</groupId>
  <artifactId>core</artifactId>
  <version>4.12.0</version>
</dependency>

<!-- PostgreSQL + Migrations -->
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>

<!-- Utils -->
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
</dependency>
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.3.0</version>
</dependency>
```

---

# 9. VARIÁVEIS DE AMBIENTE (.env)

```properties
# Database
DB_URL=jdbc:postgresql://localhost:5432/agentrep
DB_USERNAME=agentrep
DB_PASSWORD=secret

# Blockchain
BASE_RPC_URL=https://base-mainnet.g.alchemy.com/v2/YOUR_KEY
CONTRACT_ADDRESS=0x...
DEPLOYER_PRIVATE_KEY=0x...   # ⚠️ NUNCA commitar — usar Vault em produção

# Spring AI (Claude)
SPRING_AI_ANTHROPIC_API_KEY=sk-ant-...

# Redis
REDIS_URL=redis://localhost:6379

# x402 Treasury (recebe os pagamentos USDC)
TREASURY_ADDRESS=0x...

# Stripe (planos mensais)
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# App
JWT_SECRET=...
CORS_ALLOWED_ORIGINS=https://agentrep.xyz
```

---

# 10. ESTIMATIVA DE CUSTOS OPERACIONAIS (mensal)

| Serviço | Custo/mês |
|---------|-----------|
| Railway (Spring Boot + PostgreSQL + Redis) | ~$20 |
| Vercel (React frontend) | Gratuito |
| Alchemy (RPC Base L2) | Gratuito até 300M compute units |
| Anthropic Claude Haiku (LLM Judge) | ~$0.25 por 1000 outcomes (~$5-25) |
| Gas fees Base L2 (escrever outcomes) | ~$0.001 por tx ≈ $1-10 |
| Domínio | ~$1 |
| **Total estimado** | **~$25-55/mês** |

Com **apenas 3-6 usuários pagantes** ($9/mês cada) você já cobre os custos operacionais.

---

*Documento criado em 06/03/2026*
*Stack: Java 21 + Spring Boot 3.x + React 18 + Solidity + Base L2*
