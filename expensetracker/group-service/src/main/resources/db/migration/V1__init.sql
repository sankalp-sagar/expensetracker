-- group-service · V1__init.sql
CREATE TABLE IF NOT EXISTS groups (
    id                UUID PRIMARY KEY,
    name              VARCHAR(150) NOT NULL,
    description       VARCHAR(500),
    avatar_url        VARCHAR(500),
    owner_id          UUID         NOT NULL,
    default_currency  VARCHAR(3)   NOT NULL DEFAULT 'USD',
    invite_code       VARCHAR(12)  UNIQUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMPTZ
);
CREATE INDEX        idx_group_owner       ON groups (owner_id);
CREATE UNIQUE INDEX idx_group_invite_code ON groups (invite_code);

CREATE TABLE IF NOT EXISTS group_members (
    id          UUID PRIMARY KEY,
    group_id    UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    role        VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_group_member UNIQUE (group_id, user_id)
);
CREATE INDEX idx_groupmember_user ON group_members (user_id);
