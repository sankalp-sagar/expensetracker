-- analytics-service · V1__init.sql
CREATE TABLE IF NOT EXISTS expense_facts (
    id           UUID PRIMARY KEY,
    expense_id   UUID         NOT NULL UNIQUE,
    payer_id     UUID         NOT NULL,
    group_id     UUID,
    amount       NUMERIC(18,2) NOT NULL,
    currency     VARCHAR(3)   NOT NULL,
    description  VARCHAR(255),
    fact_date    DATE         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ
);
CREATE INDEX idx_fact_payer ON expense_facts (payer_id);
CREATE INDEX idx_fact_group ON expense_facts (group_id);
CREATE INDEX idx_fact_date  ON expense_facts (fact_date);
