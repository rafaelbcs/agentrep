-- AgentRep Initial Schema
-- V1: agents, outcomes, disputes, payments, agent_category_scores

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE agents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_address  VARCHAR(42) NOT NULL UNIQUE,
    name            VARCHAR(100),
    description     TEXT,
    owner_email     VARCHAR(255),
    api_key_hash    VARCHAR(255) NOT NULL,
    tier            VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    score           NUMERIC(5,2) NOT NULL DEFAULT 0,
    total_outcomes  INTEGER NOT NULL DEFAULT 0,
    success_rate    NUMERIC(5,4) NOT NULL DEFAULT 0,
    on_chain_synced BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE outcomes (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contractor_agent_id       UUID NOT NULL REFERENCES agents(id),
    requester_agent_id        UUID NOT NULL REFERENCES agents(id),
    task_description          TEXT NOT NULL,
    task_category             VARCHAR(50) NOT NULL,
    deliverable_url           TEXT,
    deliverable_hash          VARCHAR(66),
    deliverable_content       TEXT,
    value_usdc                NUMERIC(18,6),
    requester_tx_hash         VARCHAR(66),
    requester_signature       TEXT,
    status                    VARCHAR(20) NOT NULL DEFAULT 'EVALUATING',
    verdict                   VARCHAR(20),
    llm_confidence            NUMERIC(4,3),
    llm_reasoning             TEXT,
    on_chain_tx_hash          VARCHAR(66),
    on_chain_registered_at    TIMESTAMPTZ,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE disputes (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outcome_id                  UUID NOT NULL REFERENCES outcomes(id),
    opened_by_agent_id          UUID NOT NULL REFERENCES agents(id),
    status                      VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    reason                      TEXT,
    evidence_url                TEXT,
    stake_payment_tx_hash       VARCHAR(66),
    counterparty_stake_tx_hash  VARCHAR(66),
    resolved_verdict            VARCHAR(30),
    resolved_reason             TEXT,
    resolved_by                 VARCHAR(42),
    deadline                    TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at                 TIMESTAMPTZ
);

CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tx_hash         VARCHAR(66) NOT NULL UNIQUE,
    payer_address   VARCHAR(42),
    amount_usdc     NUMERIC(18,6),
    endpoint        VARCHAR(255),
    used            BOOLEAN NOT NULL DEFAULT FALSE,
    validated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE agent_category_scores (
    agent_id        UUID NOT NULL REFERENCES agents(id),
    category        VARCHAR(50) NOT NULL,
    score           NUMERIC(5,2) NOT NULL DEFAULT 0,
    total_outcomes  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (agent_id, category)
);

-- Indexes
CREATE INDEX idx_agents_wallet       ON agents(wallet_address);
CREATE INDEX idx_agents_tier_score   ON agents(tier, score DESC);
CREATE INDEX idx_agents_score        ON agents(score DESC);
CREATE INDEX idx_outcomes_contractor ON outcomes(contractor_agent_id, created_at DESC);
CREATE INDEX idx_outcomes_status     ON outcomes(status);
CREATE INDEX idx_outcomes_category   ON outcomes(task_category);
CREATE INDEX idx_disputes_outcome    ON disputes(outcome_id);
CREATE INDEX idx_disputes_status     ON disputes(status);
CREATE INDEX idx_payments_tx         ON payments(tx_hash);

-- Auto-update updated_at on agents
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_agents_updated_at
    BEFORE UPDATE ON agents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
