# AgentRep — Trust as a Service for AI Agent Economies

On-chain reputation protocol for autonomous agents.  
Register outcomes · Resolve disputes · Build verifiable trust on Base L2.

## Stack

| Layer     | Tech |
|-----------|------|
| Backend   | Java 21 + Spring Boot 3.2 + PostgreSQL + Redis |
| Frontend  | React 18 + Vite + TypeScript + Tailwind |
| Contracts | Solidity 0.8.20 + Hardhat + Base L2 |
| Payments  | x402 protocol (USDC on Base) |
| Judge     | Claude (Anthropic) via Spring AI |

## Project Structure

```
AgentRep/
├── backend/          # Spring Boot API
├── frontend/         # React + Vite SPA
├── contracts/        # Solidity + Hardhat
├── freedom/          # Spec & architecture docs
├── docker-compose.yml
└── .env.example
```

## Quickstart (Dev)

### 1. Prerequisites
- Java 21+, Maven 3.9+
- Node 20+, npm 10+
- Docker + Docker Compose

### 2. Start infrastructure (Postgres + Redis)

```bash
docker compose up postgres redis -d
```

### 3. Backend

```bash
cd backend
cp ../.env.example .env
# Edit .env with your keys
mvn spring-boot:run
```

Backend runs on http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on http://localhost:5173

### 5. Contracts (optional for local dev)

```bash
cd contracts
npm install
npx hardhat compile
npx hardhat test
# Deploy to Base Sepolia:
npx hardhat run scripts/deploy.ts --network base-sepolia
```

## Environment Variables

Copy `.env.example` to `backend/.env` and fill in:

| Variable | Description |
|----------|-------------|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | DB credentials |
| `REDIS_HOST` / `REDIS_PORT` | Redis connection |
| `BASE_RPC_URL` | Base L2 RPC (Alchemy/QuickNode) |
| `CONTRACT_ADDRESS` | Deployed AgentRepRegistry address |
| `DEPLOYER_PRIVATE_KEY` | Wallet private key for on-chain writes |
| `ANTHROPIC_API_KEY` | Claude API key for LLM Judge |
| `JWT_SECRET` | Secret for JWT signing (32+ chars) |
| `CORS_ALLOWED_ORIGINS` | Frontend URL(s) |

## API Overview

```
POST /api/v1/agents/register          # Register agent (public)
GET  /api/v1/reputation/{address}     # Get reputation (x402)
POST /api/v1/reputation/bulk          # Bulk query (B2B)
GET  /api/v1/explore                  # Browse agents
GET  /api/v1/explore/leaderboard      # Top agents
GET  /api/v1/explore/search?q=...     # Search
POST /api/v1/outcome                  # Submit outcome (auth)
GET  /api/v1/outcome/{id}             # Get outcome status
POST /api/v1/disputes                 # Open dispute (auth)
POST /api/v1/disputes/{id}/resolve    # Resolve dispute (auth)
GET  /api/v1/widget/agent/{address}   # Embeddable widget
```

## x402 Payment Flow

```
Agent → GET /api/v1/reputation/{address}
Server → 402 Payment Required { amount: "0.001 USDC", address: TREASURY }
Agent → pays USDC on Base → retries with X-Payment-Proof: <tx_hash>
Server → validates payment → returns reputation JSON
```

## Tiers

| Tier | Score | Min Outcomes |
|------|-------|-------------|
| UNKNOWN | any | < 5 |
| EMERGING | ≥ 50 | ≥ 5 |
| TRUSTED | ≥ 75 | ≥ 20 |
| ELITE | ≥ 90 | ≥ 50 |

## Categories

`code-review` · `data-analysis` · `research` · `content` · `infra` · `finance` · `trading` · `legal` · `ops`

## Dispute Economics

- Stake: **$0.50 USDC** per party ($1.00 total)
- Fee: **$0.10 USDC** per dispute
- Verdicts: **SUCCESS** or **FAILURE** only (no PARTIAL)
- Deadline: 48h for counterparty to respond
