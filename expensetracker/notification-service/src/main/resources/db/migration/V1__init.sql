-- notification-service · V1__init.sql
CREATE TABLE IF NOT EXISTS notifications (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        VARCHAR(1000),
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    related_id  UUID,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ
);
CREATE INDEX idx_notif_user ON notifications (user_id);
CREATE INDEX idx_notif_read ON notifications (is_read);
