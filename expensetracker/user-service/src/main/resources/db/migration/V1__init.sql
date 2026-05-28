-- user-service · V1__init.sql
CREATE TABLE IF NOT EXISTS user_profiles (
    id                   UUID PRIMARY KEY,
    user_id              UUID         NOT NULL UNIQUE,
    email                VARCHAR(320) NOT NULL,
    full_name            VARCHAR(150),
    avatar_url           VARCHAR(500),
    status_message       VARCHAR(280),
    phone                VARCHAR(30),
    preferred_currency   VARCHAR(3)  DEFAULT 'USD',
    preferred_language   VARCHAR(5)  DEFAULT 'en',
    privacy              VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted              BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_profile_user  ON user_profiles (user_id);
CREATE INDEX        idx_profile_email ON user_profiles (email);

CREATE TABLE IF NOT EXISTS friendships (
    id            UUID PRIMARY KEY,
    requester_id  UUID NOT NULL,
    addressee_id  UUID NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT uq_friendship UNIQUE (requester_id, addressee_id)
);
CREATE INDEX idx_friendship_requester ON friendships (requester_id);
CREATE INDEX idx_friendship_addressee ON friendships (addressee_id);
