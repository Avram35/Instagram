CREATE TABLE IF NOT EXISTS blocks (
    id              BIGSERIAL       PRIMARY KEY,
    blocker_id      BIGINT          NOT NULL,
    blocked_id      BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    CONSTRAINT uk_blocks_pair UNIQUE (blocker_id, blocked_id)
);