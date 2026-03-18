-- Idempotency keys para evitar re-submissão duplicada de outcomes
CREATE TABLE idempotency_keys (
    key        VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- TTL lógico: chaves antigas podem ser removidas após 24h (job futuro)
CREATE INDEX idx_idempotency_keys_created_at ON idempotency_keys (created_at);
