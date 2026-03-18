#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# AgentRep — Seed Script
# Seeds the local dev environment with:
#   - 2 agents (contractor + requester)
#   - 3 outcomes (2 SUCCESS-worthy, 1 FAILURE-worthy)
#   - 1 dispute
#
# Requirements: bash, curl  (no jq needed)
# Usage: bash scripts/seed.sh [API_BASE]
#   API_BASE defaults to http://localhost:8080/api/v1
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

API="${1:-http://localhost:8080/api/v1}"
RED='\033[0;31m'; GREEN='\033[0;32m'; BLUE='\033[0;34m'; YELLOW='\033[1;33m'; NC='\033[0m'

log()  { echo -e "${BLUE}▶${NC} $*"; }
ok()   { echo -e "${GREEN}✓${NC} $*"; }
warn() { echo -e "${YELLOW}!${NC} $*"; }
fail() { echo -e "${RED}✗${NC} $*"; exit 1; }

# Extract a string value from a JSON response without jq
# Usage: json_get KEY JSON_STRING
json_get() {
  local key="$1"
  local json="$2"
  echo "$json" | grep -o "\"${key}\":\"[^\"]*\"" | sed "s/\"${key}\":\"//;s/\"$//"
}

command -v curl &>/dev/null || fail "curl is required"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  AgentRep — Dev Seed${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# ── Health check ──────────────────────────────────────────────────────────────
log "Checking backend health..."
BASE_URL="${API%/api/v1}"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
if [[ "$HTTP" != "200" ]]; then
  # Fallback: try hitting the register endpoint (always public)
  HTTP2=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API/agents/register" 2>/dev/null || echo "000")
  if [[ "$HTTP2" == "000" ]]; then
    fail "Backend not responding at $API. Run: cd backend && mvn spring-boot:run"
  fi
fi
ok "Backend is up ($BASE_URL)"
echo ""

# ── Hardhat test addresses (deterministic, safe to commit) ───────────────────
CONTRACTOR_ADDR="0x70997970C51812dc3A010C7d01b50e0d17dc79C8"
REQUESTER_ADDR="0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC"

# ─────────────────────────────────────────────────────────────────────────────
# 1. Register contractor agent
# ─────────────────────────────────────────────────────────────────────────────
log "Registering contractor agent ($CONTRACTOR_ADDR)..."

CONTRACTOR_RESP=$(curl -s -X POST "$API/agents/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"agentAddress\": \"$CONTRACTOR_ADDR\",
    \"name\": \"CodeCraft AI\",
    \"description\": \"Specialized in code review and software architecture analysis.\",
    \"ownerEmail\": \"dev@codecraft.ai\",
    \"categories\": [\"code-review\", \"infra\", \"data-analysis\"]
  }")

CONTRACTOR_KEY=$(json_get "apiKey" "$CONTRACTOR_RESP")
CONTRACTOR_ID=$(json_get "agentId" "$CONTRACTOR_RESP")

if [[ -z "$CONTRACTOR_KEY" ]]; then
  warn "Contractor may already exist. Attempting to continue..."
else
  ok "Contractor registered — ID: $CONTRACTOR_ID"
  echo "   API Key: $CONTRACTOR_KEY"
fi
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# 2. Register requester agent
# ─────────────────────────────────────────────────────────────────────────────
log "Registering requester agent ($REQUESTER_ADDR)..."

REQUESTER_RESP=$(curl -s -X POST "$API/agents/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"agentAddress\": \"$REQUESTER_ADDR\",
    \"name\": \"DataFlow Orchestrator\",
    \"description\": \"Orchestrates multi-agent data pipelines and quality checks.\",
    \"ownerEmail\": \"ops@dataflow.ai\",
    \"categories\": [\"data-analysis\", \"ops\", \"research\"]
  }")

REQUESTER_KEY=$(json_get "apiKey" "$REQUESTER_RESP")
REQUESTER_ID=$(json_get "agentId" "$REQUESTER_RESP")

if [[ -z "$REQUESTER_KEY" ]]; then
  warn "Requester may already exist. Attempting to continue..."
else
  ok "Requester registered — ID: $REQUESTER_ID"
  echo "   API Key: $REQUESTER_KEY"
fi
echo ""

if [[ -z "$CONTRACTOR_KEY" ]]; then
  fail "Cannot proceed without contractor API key (agent already registered?). Clean the DB and retry."
fi

# ─────────────────────────────────────────────────────────────────────────────
# 3. Submit outcomes
# ─────────────────────────────────────────────────────────────────────────────

submit_outcome() {
  local label="$1"
  local category="$2"
  local description="$3"
  local content="$4"

  log "Submitting outcome: $label..."
  RESP=$(curl -s -X POST "$API/outcome" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $CONTRACTOR_KEY" \
    -d "{
      \"contractorAgentAddress\": \"$CONTRACTOR_ADDR\",
      \"requesterAgentAddress\": \"$REQUESTER_ADDR\",
      \"taskDescription\": \"$description\",
      \"taskCategory\": \"$category\",
      \"deliverableContent\": \"$content\",
      \"valueUsdc\": 10.0
    }")

  local oid
  oid=$(json_get "outcomeId" "$RESP")
  local status
  status=$(json_get "status" "$RESP")

  if [[ -z "$oid" ]]; then
    warn "$label — submission failed (response: $RESP)"
  else
    ok "$label — ID: $oid (status: $status)"
  fi
  echo "$oid"
}

OUTCOME1=$(submit_outcome \
  "Code Review — Python function" \
  "code-review" \
  "Review this Python function for correctness and style: def fib(n): return n if n<=1 else fib(n-1)+fib(n-2)" \
  "The function is correct and clean. Uses classic recursion. For large inputs consider memoization (@lru_cache). Style: PEP 8 compliant. Verdict: APPROVED with minor performance note.")
echo ""

OUTCOME2=$(submit_outcome \
  "Data Analysis — CSV summary" \
  "data-analysis" \
  "Analyze sales.csv and produce a Q1 2025 summary with totals per region" \
  "Q1 2025 Summary: Total revenue 2.4M (+18% YoY). Top regions: APAC 820K, NA 780K, EMEA 540K. Charts: bar by region, trend line Jan-Mar. Data validated against source.")
echo ""

OUTCOME3=$(submit_outcome \
  "Infrastructure audit — incomplete" \
  "infra" \
  "Audit the Kubernetes cluster config and provide a hardening report with CVE analysis" \
  "")
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# 4. Open a dispute on outcome 3
# ─────────────────────────────────────────────────────────────────────────────
if [[ -n "$OUTCOME3" && -n "$REQUESTER_KEY" ]]; then
  log "Waiting 3s for outcome 3 to be evaluated..."
  sleep 3

  log "Opening dispute on outcome: $OUTCOME3..."
  DISPUTE_RESP=$(curl -s -X POST "$API/disputes" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $REQUESTER_KEY" \
    -d "{
      \"outcomeId\": \"$OUTCOME3\",
      \"reason\": \"Deliverable was empty. No hardening report was provided despite clear task requirements.\",
      \"stakePaymentTxHash\": \"0xseed_dispute_$(date +%s)\"
    }")

  DISPUTE_ID=$(json_get "disputeId" "$DISPUTE_RESP")
  if [[ -z "$DISPUTE_ID" ]]; then
    warn "Dispute creation failed (response: $DISPUTE_RESP)"
  else
    ok "Dispute opened — ID: $DISPUTE_ID"
  fi
else
  warn "Skipping dispute (missing outcome ID or requester key)"
fi

# ─────────────────────────────────────────────────────────────────────────────
# 5. Summary
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  Seed complete!${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "  Contractor : $CONTRACTOR_ADDR"
echo "  Requester  : $REQUESTER_ADDR"
echo ""
echo "  Explorer   : http://localhost:5173/explore"
echo "  Swagger    : http://localhost:8080/swagger-ui"
echo ""
echo "  Check reputation:"
echo "  curl $API/reputation/$CONTRACTOR_ADDR"
echo ""
