# Deploy AgentRepRegistry — Base Mainnet

**Pré-requisito:** deploy em Base Sepolia já concluído (contrato testado e funcionando).

---

## 1. Obter ETH real na Base Mainnet

Diferente da testnet, aqui você precisa de **ETH real**. ~$5–10 USD é mais do que suficiente para o deploy + primeiras centenas de transações na Base L2.

### Opção A — MoonPay (recomendado — cartão de crédito/débito)
1. Acesse **https://www.moonpay.com**
2. Clique em **Buy**
3. Selecione **ETH** e escolha a rede **Base**
4. Endereço de destino: `0x7450030057cf98CC3d9Ad924577bb2E9cEfbE440`
5. Valor: ~$10 USD (mais que suficiente)
6. Pague com cartão — entrega em ~5 minutos

> Se a rede Base não aparecer no MoonPay, compre ETH na Ethereum e use o bridge da Opção B abaixo.

### Opção B — Comprar ETH diretamente na Base via carteira
Pelo próprio MetaMask ou Coinbase Wallet, compre ETH já na rede Base:
- MetaMask → **Buy** → selecione **Base** → compre via cartão

### Opção B — Bridge da Ethereum Mainnet para Base
Se você já tiver ETH na Ethereum Mainnet:
1. Acesse **https://bridge.base.org**
2. Conecte a wallet `0x7450030057cf98CC3d9Ad924577bb2E9cEfbE440`
3. Selecione **Ethereum → Base**
4. Bridge pelo menos **0.005 ETH** (~$15)
5. Aguarde ~5 minutos para chegar

### Opção C — Superchain Bridge (mesmo site que funcionou no testnet)
1. Acesse **https://superbridge.app/base**
2. Conecte a wallet
3. Bridge de Ethereum Mainnet → Base Mainnet

> **Nota:** O faucet do Superchain (`app.optimism.io/faucet`) funciona **apenas para testnet**. Para Mainnet é necessário ETH real via bridge ou compra direta.

---

## 2. Confirmar saldo na Base Mainnet

Verifique se o ETH chegou:

```
https://basescan.org/address/0x7450030057cf98CC3d9Ad924577bb2E9cEfbE440
```

Deve mostrar saldo > 0 ETH.

---

## 3. Atualizar contracts/.env

Adicione a variável de RPC de Mainnet (o arquivo já tem as outras):

```env
DEPLOYER_PRIVATE_KEY=0xSUA_CHAVE_PRIVADA
BASE_RPC_URL=https://sepolia.base.org          # mantém para Sepolia
BASE_MAINNET_RPC_URL=https://mainnet.base.org  # adicionar esta linha
BACKEND_SIGNER_ADDRESS=0x7450030057cf98CC3d9Ad924577bb2E9cEfbE440
```

> O `BASE_MAINNET_RPC_URL` público da Base é gratuito e suficiente para o deploy.
> Se quiser mais estabilidade: crie conta gratuita no Alchemy → crie app na Base Mainnet → copie o RPC URL.

---

## 4. Compilar e fazer o deploy

```bash
cd contracts
npx hardhat compile
npx hardhat run scripts/deploy.ts --network base-mainnet
```

Output esperado:
```
Deploying AgentRepRegistry with account: 0x7450...bE440
Backend signer: 0x7450...bE440
AgentRepRegistry deployed to: 0xNOVO_ENDERECO_AQUI
Set CONTRACT_ADDRESS=0xNOVO_ENDERECO_AQUI in backend .env
```

Anote o endereço do contrato.

---

## 5. Verificar o contrato no Basescan (opcional mas recomendado)

Verificar o código-fonte deixa o contrato transparente para usuários e parceiros.

```bash
cd contracts
npx hardhat verify --network base-mainnet 0xNOVO_ENDERECO_AQUI "0x7450030057cf98CC3d9Ad924577bb2E9cEfbE440"
```

> Precisa de uma `BASESCAN_API_KEY` no `.env`. Crie gratuitamente em: **https://basescan.org/register**

---

## 6. Atualizar variáveis no Railway

No Railway → backend → **Variables**, atualize:

| Variável | Valor |
|---|---|
| `CONTRACT_ADDRESS` | `0xNOVO_ENDERECO_MAINNET` |
| `CHAIN_ID` | `8453` |
| `BASE_RPC_URL` | `https://mainnet.base.org` |

Clique em **Deploy** e aguarde o redeploy (~1 min).

---

## 7. Smoke test em Mainnet

Registre um agente e submeta um outcome via curl (igual ao teste da Sepolia):

```bash
API=https://api.agentrep.com.br/api/v1

# 1. Registrar agente
curl -s -X POST $API/agents/register \
  -H "Content-Type: application/json" \
  -d '{
    "agentAddress": "0x7450030057cf98CC3d9Ad924577bb2E9cEfbE440",
    "name": "AgentRep Demo",
    "description": "Official demo agent",
    "categories": ["code-review"]
  }'

# 2. Submeter outcome (use a apiKey retornada acima)
curl -s -X POST $API/outcome \
  -H "Content-Type: application/json" \
  -H "X-API-Key: SUA_API_KEY" \
  -d '{
    "contractorAgentAddress": "0x7450030057cf98CC3d9Ad924577bb2E9cEfbE440",
    "requesterAgentAddress": "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC",
    "taskDescription": "Review Python function: def add(a, b): return a + b",
    "taskCategory": "code-review",
    "deliverableContent": "Function is correct and follows PEP 8. Spaces after comma and around operator. No issues found.",
    "valueUsdc": 5.0
  }'
```

Verifique a transação em: **https://basescan.org** (sem "sepolia.")

---

## 8. Checklist final antes do lançamento

- [ ] ETH real na wallet de deploy na Base Mainnet
- [ ] Contrato deployado e endereço anotado
- [ ] Railway atualizado com `CONTRACT_ADDRESS` e `CHAIN_ID=8453`
- [ ] Smoke test: outcome com `onChainTx` retornando hash real
- [ ] Transação visível em `https://basescan.org/tx/SEU_HASH`
- [ ] Contrato verificado no Basescan (opcional)

---

Após concluir este checklist, o sistema está pronto para receber **agentes e transações reais**.
