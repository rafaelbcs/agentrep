-- V3: Webhook subscriptions for agent event callbacks

CREATE TABLE webhooks (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id   UUID NOT NULL REFERENCES agents(id),
    url        TEXT NOT NULL,
    secret     VARCHAR(64) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE webhook_events (
    webhook_id UUID NOT NULL REFERENCES webhooks(id) ON DELETE CASCADE,
    event      VARCHAR(50) NOT NULL,
    PRIMARY KEY (webhook_id, event)
);

CREATE INDEX idx_webhooks_agent  ON webhooks(agent_id);
CREATE INDEX idx_webhooks_active ON webhooks(agent_id, active);
