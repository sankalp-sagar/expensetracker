-- expense-service · V1__init.sql
CREATE TABLE IF NOT EXISTS categories (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    icon        VARCHAR(50),
    color_hex   VARCHAR(7),
    owner_id    UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_category_name ON categories (name);

CREATE TABLE IF NOT EXISTS expenses (
    id                  UUID PRIMARY KEY,
    group_id            UUID,
    payer_id            UUID         NOT NULL,
    description         VARCHAR(255) NOT NULL,
    amount              NUMERIC(18,2) NOT NULL,
    currency            VARCHAR(3)   NOT NULL DEFAULT 'USD',
    expense_date        DATE         NOT NULL DEFAULT CURRENT_DATE,
    category_id         UUID REFERENCES categories(id),
    split_type          VARCHAR(20)  NOT NULL,
    is_recurring        BOOLEAN      NOT NULL DEFAULT FALSE,
    recurrence_period   VARCHAR(20),
    next_occurrence     DATE,
    notes               VARCHAR(1000),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_expense_group ON expenses (group_id);
CREATE INDEX idx_expense_payer ON expenses (payer_id);
CREATE INDEX idx_expense_date  ON expenses (expense_date);

CREATE TABLE IF NOT EXISTS expense_splits (
    id          UUID PRIMARY KEY,
    expense_id  UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    amount      NUMERIC(18,2) NOT NULL,
    raw_value   NUMERIC(18,6),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ
);
CREATE INDEX idx_split_user    ON expense_splits (user_id);
CREATE INDEX idx_split_expense ON expense_splits (expense_id);

CREATE TABLE IF NOT EXISTS receipts (
    id            UUID PRIMARY KEY,
    expense_id    UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    storage_key   VARCHAR(500) NOT NULL,
    file_name     VARCHAR(255),
    content_type  VARCHAR(100),
    size_bytes    BIGINT,
    ocr_text      TEXT,
    ocr_processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ
);
CREATE INDEX idx_receipt_expense ON receipts (expense_id);

-- seed common categories
INSERT INTO categories (id, name, icon, color_hex, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Food & Drink',   'utensils', '#E5484D', NOW(), NOW()),
    (gen_random_uuid(), 'Transport',      'car',      '#0055FF', NOW(), NOW()),
    (gen_random_uuid(), 'Groceries',      'shopping-cart', '#30A46C', NOW(), NOW()),
    (gen_random_uuid(), 'Rent & Utilities','home',    '#F5A524', NOW(), NOW()),
    (gen_random_uuid(), 'Entertainment',  'film',     '#9333EA', NOW(), NOW()),
    (gen_random_uuid(), 'Travel',         'plane',    '#06B6D4', NOW(), NOW()),
    (gen_random_uuid(), 'Other',          'tag',      '#71717A', NOW(), NOW())
ON CONFLICT DO NOTHING;
