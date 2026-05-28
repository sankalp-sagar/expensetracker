-- settlement-service · V1__init.sql
CREATE TABLE IF NOT EXISTS balances (
    id          UUID PRIMARY KEY,
    group_id    UUID,
    user_a      UUID NOT NULL,
    user_b      UUID NOT NULL,
    amount      NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency    VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_balance UNIQUE (group_id, user_a, user_b, currency)
);
CREATE INDEX idx_balance_group ON balances (group_id);
CREATE INDEX idx_balance_a     ON balances (user_a);
CREATE INDEX idx_balance_b     ON balances (user_b);

CREATE TABLE IF NOT EXISTS settlements (
    id          UUID PRIMARY KEY,
    group_id    UUID,
    payer_id    UUID NOT NULL,
    payee_id    UUID NOT NULL,
    amount      NUMERIC(18,2) NOT NULL,
    currency    VARCHAR(3)  NOT NULL DEFAULT 'USD',
    status      VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    settled_at  TIMESTAMPTZ,
    method      VARCHAR(30),
    note        VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ
);
CREATE INDEX idx_settle_payer ON settlements (payer_id);
CREATE INDEX idx_settle_payee ON settlements (payee_id);
CREATE INDEX idx_settle_group ON settlements (group_id);
