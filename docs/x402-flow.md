# x402 Payment Flow

> **Current status:** queries are **FREE at launch**.
> x402 will be enabled after 50+ agents with history.
> This document describes the flow for when it activates.

---

## What is x402?

x402 is an HTTP payment protocol: the server responds `402 Payment Required` with
payment instructions, the client pays, and retries with a payment proof.

Native to AI agents — no wallets popups, no OAuth, just HTTP headers.

---

## Flow: querying reputation (paid)

```
Agent                          AgentRep API
  │                                 │
  │── GET /reputation/0x...  ──────>│
  │                                 │
  │<── 402 Payment Required ────────│
  │    X-Payment-Network: base      │
  │    X-Payment-Amount: 0.001      │
  │    X-Payment-Token: USDC        │
  │    X-Payment-Recipient: 0x...   │
  │    X-Payment-Nonce: abc123      │
  │                                 │
  │  [Agent pays 0.001 USDC on Base]│
  │                                 │
  │── GET /reputation/0x...  ──────>│
  │   X-Payment-Proof: 0xtxhash    │
  │                                 │
  │<── 200 OK + reputation JSON ────│
```

---

## Price table (when enabled)

| Operation              | Price    | Auth required |
|------------------------|----------|---------------|
| `GET /reputation/{addr}` | $0.001 USDC | No |
| `POST /reputation/bulk`  | $0.001 × N | Yes (API Key) |
| `POST /outcome`          | Free     | Yes |
| `POST /disputes`         | $0.50 stake + $0.10 fee | Yes |

---

## Paying with ethers.js

```js
import { ethers } from "ethers";

// USDC on Base
const USDC = "0x036CbD53842c5426634e7929541eC2318f3dCF7e"; // Base Sepolia
const usdcAbi = ["function transfer(address to, uint256 amount) returns (bool)"];

async function payAndQuery(agentAddress, signer) {
  // Step 1: try the request
  const res = await fetch(`https://api.agentrep.com.br/api/v1/reputation/${agentAddress}`);

  if (res.status !== 402) return await res.json();

  const instructions = {
    recipient: res.headers.get("X-Payment-Recipient"),
    amount:    res.headers.get("X-Payment-Amount"),   // "0.001"
    nonce:     res.headers.get("X-Payment-Nonce"),
  };

  // Step 2: pay
  const usdc = new ethers.Contract(USDC, usdcAbi, signer);
  const amount = ethers.parseUnits(instructions.amount, 6); // USDC has 6 decimals
  const tx = await usdc.transfer(instructions.recipient, amount);
  await tx.wait();

  // Step 3: retry with proof
  const retry = await fetch(`https://api.agentrep.com.br/api/v1/reputation/${agentAddress}`, {
    headers: { "X-Payment-Proof": tx.hash }
  });

  return await retry.json();
}
```

---

## Paying with Python (web3.py)

```python
import requests
from web3 import Web3

def query_reputation(agent_address: str, w3: Web3, account, private_key: str) -> dict:
    url = f"https://api.agentrep.com.br/api/v1/reputation/{agent_address}"

    # Step 1: try
    res = requests.get(url)
    if res.status_code != 402:
        return res.json()

    recipient = res.headers["X-Payment-Recipient"]
    amount    = int(float(res.headers["X-Payment-Amount"]) * 1_000_000)  # USDC 6 decimals

    # Step 2: pay (transfer USDC)
    usdc = w3.eth.contract(address=USDC_ADDRESS, abi=USDC_ABI)
    tx   = usdc.functions.transfer(recipient, amount).build_transaction({
        "from":  account.address,
        "nonce": w3.eth.get_transaction_count(account.address),
    })
    signed = w3.eth.account.sign_transaction(tx, private_key)
    tx_hash = w3.eth.send_raw_transaction(signed.raw_transaction).hex()
    w3.eth.wait_for_transaction_receipt(tx_hash)

    # Step 3: retry with proof
    return requests.get(url, headers={"X-Payment-Proof": tx_hash}).json()
```

---

## Anti-replay

Each payment nonce is stored and cannot be reused.
Reusing a proof returns `HTTP 402` again (new payment required).

---

## Testing (when x402 is live)

```bash
# Expect 402 with instructions
curl -v https://api.agentrep.com.br/api/v1/reputation/0x...
# → HTTP/1.1 402 Payment Required
# → X-Payment-Amount: 0.001
# → X-Payment-Token: USDC
```
