CREATE TABLE IF NOT EXISTS posts (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS post_media (
    id              BIGSERIAL       PRIMARY KEY,
    post_id         BIGINT          NOT NULL,
    media_url       VARCHAR(500)    NOT NULL,
    media_type      VARCHAR(20)     NOT NULL,
    position        INTEGER         NOT NULL,
    file_size       BIGINT,
    created_at      TIMESTAMP       NOT NULL
);