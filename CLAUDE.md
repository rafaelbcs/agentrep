# AgentRep — Guia de Desenvolvimento para Claude Code

## Contexto do Projeto
**AgentRep** é um primitivo de confiança ("Trust as a Service") para agentes de IA em Web3.
- Backend: Java 21 + Spring Boot 3.2 (`/backend`)
- Frontend: React 18 + TypeScript + Tailwind (`/frontend`)
- Contratos: Solidity 0.8.20 + Hardhat (`/contracts`)
- DB: PostgreSQL + Redis
- LLM Judge: Claude Sonnet 4.6 via Spring AI
- Pagamentos: x402 (USDC na Base L2)
- Blockchain: Base L2 (testnet: Base Sepolia, chainId 84532)

## Status Atual
- Fase 0 ✅ concluída — scaffolding completo
- Fase 1 em andamento — LLM Judge ✅ concluído
- Próximo: Web3j integration → Deploy Base Sepolia → WebhookService → Frontend

## Cronograma
Ver: `freedom/AgentRep/cronograma.md`
Especificação: `freedom/AgentRep/agentrep-especificacao.md`
Arquitetura: `freedom/AgentRep/agentrep-arquitetura.md`

---

## Workflow de Desenvolvimento

### Início de cada sessão
```
/resume-session    ← recuperar contexto da sessão anterior
```

### Por tarefa do cronograma

1. **Planejamento** (features com > 3 arquivos afetados)
   ```
   /make-plan    ← plano faseado com riscos
   ```

2. **Exploração** (entender código existente antes de modificar)
   ```
   /smart-explore
   ```

3. **Backend (Java)** — sempre TDD
   ```
   /springboot-tdd           ← escrever testes PRIMEIRO
   → implementar
   /springboot-verification  ← build + testes + lint ao final
   ```

4. **Contrato (Solidity)**
   ```
   /ethskills:testing    ← testes primeiro (Foundry/Hardhat)
   → implementar
   /ethskills:security   ← verificar vulnerabilidades
   ```

5. **Segurança** (obrigatório para auth, pagamentos, blockchain)
   ```
   /security-review
   ```

6. **Pré-deploy de contrato**
   ```
   /ethskills:qa      ← checklist completo
   /ethskills:ship    ← fluxo deploy testnet → mainnet
   ```

7. **Fim de sessão**
   ```
   /save-session    ← persistir estado
   /learn-eval      ← extrair padrões
   ```

---

## Regras Obrigatórias

| Gatilho | Ação |
|---|---|
| Nova sessão | `/resume-session` primeiro |
| Feature com > 3 arquivos | `/make-plan` antes de codar |
| Qualquer mudança Java | `/springboot-verification` ao final |
| Código auth / API key / JWT / pagamento | `/security-review` obrigatório |
| Qualquer mudança `.sol` | `/ethskills:security` + recompila |
| Antes de `hardhat deploy` | `/ethskills:qa` |
| Encerrar sessão | `/save-session` + `/learn-eval` |

---

## Plugins instalados
- `everything-claude-code` — padrões Java/Spring, TDD, segurança, verificação
- `ethskills` — conhecimento Ethereum/Base L2 atualizado (2026)
- `claude-mem` — memória persistente e planejamento faseado

## Decisões arquiteturais fixas (não questionar)
- Veredictos LLM Judge: apenas `SUCCESS` / `FAILURE` (sem PARTIAL)
- Stake de disputa: fixo $0.50 USDC por parte
- Consulta de reputação: gratuita no lançamento
- Modelo LLM: `claude-sonnet-4-6`
- Chain: Base L2 (Sepolia para dev, Mainnet para prod)
- ERC-8004: AgentRep é a camada de avaliação sobre o ERC-8004 (não concorre)

## Variáveis de ambiente
Ver: `.env` (nunca commitar)
Template: `.env.example`

## Comandos úteis
```bash
# Backend
cd backend && mvn compile              # compilar
cd backend && mvn test -Dtest=NomeIT   # rodar teste específico

# Contratos
cd contracts && npx hardhat compile    # compilar
cd contracts && npx hardhat test       # testar

# Infra
docker compose up postgres redis -d    # subir DB + Redis
```
