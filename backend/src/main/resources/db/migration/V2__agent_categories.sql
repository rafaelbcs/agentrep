-- V2: agent_categories table for ElementCollection mapping

CREATE TABLE agent_categories (
    agent_id UUID NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    PRIMARY KEY (agent_id, category)
);

CREATE INDEX idx_agent_categories_category ON agent_categories(category);
