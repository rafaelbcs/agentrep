# Deploy AgentRep — Railway + Vercel + Base Sepolia

> Guia completo e atualizado. Execute na ordem abaixo.

---

## Pré-requisitos

- Conta no [Railway](https://railway.app)
- Conta no [Vercel](https://vercel.com)
- Wallet MetaMask com ETH na Base Sepolia (para gas)
- Variáveis prontas: chave privada do deployer, API key da Anthropic

---

## 1. Infraestrutura no Railway

### 1.1 Criar projeto

1. Railway → **New Project → Empty Project**
2. Renomear para `agentrep`

### 1.2 Adicionar PostgreSQL

1. **+ New → Database → PostgreSQL**
2. Aguardar provisionar
3. Clicar no plugin Postgres → aba **Variables** — anote os valores:
   - `PGHOST` (ex: `postgres.railway.internal`)
   - `PGPORT` (ex: `5432`)
   - `PGDATABASE` (ex: `railway`)
   - `PGUSER` (ex: `postgres`)
   - `PGPASSWORD` (ex: `wETWgEPcOOgKslvkUGrxUJgmjEVnXdKo`)

> Também disponível como `DATABASE_URL` no formato `postgresql://user:pass@host:port/db`

### 1.3 Adicionar Redis

1. **+ New → Database → Redis**
2. Aguardar provisionar
3. Anotar `REDIS_URL` (ex: `redis://default:senha@redis.railway.internal:6379`)

---

## 2. Deploy do Backend (Spring Boot)

### 2.1 Criar serviço backend

1. **+ New → GitHub Repo** → selecionar o repositório AgentRep
2. Railway detectará o `Dockerfile` ou usará Nixpacks — se necessário, configurar:
   - **Root Directory:** `backend`
   - **Build Command:** `mvn package -DskipTests`
   - **Start Command:** `java -jar target/*.jar`

### 2.2 Configurar variáveis de ambiente

No serviço backend → aba **Variables**, adicionar:

#### Banco de dados
> ⚠️ **Importante:** O Railway fornece `DATABASE_URL` no formato `postgresql://...` que não funciona com Spring/Flyway.
> Monte a URL JDBC **manualmente** com os valores copiados do plugin Postgres.

| Variável | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://postgres.railway.internal:5432/railway` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `wETWgEPcOOgKslvkUGrxUJgmjEVnXdKo` |

> Substitua host, porta, database, usuário e senha pelos valores reais do seu plugin.

#### Redis
Extrair host e porta do `REDIS_URL` (`redis://default:senha@host:porta`):

| Variável | Valor |
|---|---|
| `REDIS_HOST` | `redis.railway.internal` |
| `REDIS_PORT` | `6379` |

#### LLM
| Variável | Valor |
|---|---|
| `ANTHROPIC_API_KEY` | `sk-ant-...` |

#### Blockchain
| Variável | Valor |
|---|---|
| `BASE_RPC_URL` | `https://sepolia.base.org` |
| `CONTRACT_ADDRESS` | *(preencher após deploy do contrato — passo 4)* |
| `DEPLOYER_PRIVATE_KEY` | `0x...` (chave privada da wallet deployer) |
| `CHAIN_ID` | `84532` |
| `TREASURY_ADDRESS` | `0x...` (endereço da wallet treasury) |

#### Segurança
| Variável | Valor |
|---|---|
| `JWT_SECRET` | string aleatória com 32+ caracteres |
| `CORS_ALLOWED_ORIGINS` | `https://agentrep.vercel.app` *(atualizar após deploy frontend)* |

### 2.3 Verificar deploy

Após salvar as variáveis, o Railway redeploya automaticamente.

Checar nos logs:
```
Started AgentrepApplication in X seconds
Flyway: Successfully applied N migrations
```

Testar health:
```bash
curl https://seu-backend.railway.app/actuator/health
# Esperado: {"status":"UP"}
```

---

## 3. Deploy do Frontend (Vercel)

### 3.1 Importar projeto

1. Vercel → **New Project → Import Git Repository**
2. Selecionar o repositório → **Root Directory:** `frontend`
3. Framework: **Vite**

### 3.2 Variáveis de ambiente no Vercel

| Variável | Valor |
|---|---|
| `VITE_API_URL` | `https://seu-backend.railway.app` |
| `VITE_CHAIN_ID` | `84532` |
| `VITE_CONTRACT_ADDRESS` | *(preencher após deploy do contrato)* |

### 3.3 Deploy

Clicar em **Deploy**. Após concluir, copiar a URL gerada (ex: `https://agentrep.vercel.app`).

Voltar ao Railway e atualizar `CORS_ALLOWED_ORIGINS` com essa URL.

---

## 4. Deploy do Contrato (Base Sepolia)

### 4.1 Configurar ambiente local

```bash
cd contracts
cp .env.example .env
```

Editar `.env`:
```env
PRIVATE_KEY=0x...          # chave privada com ETH na Base Sepolia
BASE_SEPOLIA_RPC=https://sepolia.base.org
```

### 4.2 Obter ETH de teste

Faucet Base Sepolia: https://www.coinbase.com/faucets/base-ethereum-goerli-faucet

### 4.3 Compilar e fazer deploy

```bash
npx hardhat compile
npx hardhat run scripts/deploy.js --network base-sepolia
```

Anotar o endereço do contrato na saída:
```
AgentRep deployed to: 0xABCD...
```

### 4.4 Atualizar variáveis

- Railway backend: `CONTRACT_ADDRESS = 0xABCD...`
- Vercel frontend: `VITE_CONTRACT_ADDRESS = 0xABCD...`

Após atualizar no Railway, aguardar redeploy automático.

---

## 5. Smoke Test em Produção

```bash
export API=https://seu-backend.railway.app

# 1. Health
curl $API/actuator/health

# 2. Registrar agente
curl -X POST $API/api/v1/agents/register \
  -H "Content-Type: application/json" \
  -d '{"agentAddress":"0x1234...","name":"TestAgent","description":"Smoke test"}'

# 3. Consultar reputação
curl "$API/api/v1/reputation/0x1234..."

# 4. Métricas Prometheus
curl $API/actuator/prometheus | grep agentrep_
```

---

## 6. DNS (opcional)

Para usar `api.agentrep.com.br` e `agentrep.com.br`:

1. **Railway** → Settings → Domains → Add custom domain: `api.agentrep.com.br`
2. **Vercel** → Settings → Domains → Add: `agentrep.com.br`
3. No painel DNS do domínio, adicionar os registros CNAME fornecidos por cada plataforma
4. Atualizar `CORS_ALLOWED_ORIGINS` no Railway com `https://agentrep.com.br`

---

## Resumo das variáveis por serviço

### Railway (backend)
```
DB_URL=jdbc:postgresql://<PGHOST>:<PGPORT>/<PGDATABASE>
DB_USERNAME=<PGUSER>
DB_PASSWORD=<PGPASSWORD>
REDIS_HOST=<redis host>
REDIS_PORT=6379
ANTHROPIC_API_KEY=sk-ant-...
BASE_RPC_URL=https://sepolia.base.org
CONTRACT_ADDRESS=0x...
DEPLOYER_PRIVATE_KEY=0x...
CHAIN_ID=84532
TREASURY_ADDRESS=0x...
JWT_SECRET=<32+ chars>
CORS_ALLOWED_ORIGINS=https://agentrep.vercel.app
```

### Vercel (frontend)
```
VITE_API_URL=https://seu-backend.railway.app
VITE_CHAIN_ID=84532
VITE_CONTRACT_ADDRESS=0x...
```
