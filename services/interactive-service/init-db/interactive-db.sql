CREATE TABLE IF NOT EXISTS likes (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    post_id         BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    CONSTRAINT uk_likes_user_post UNIQUE (user_id, post_id)
);

CREATE TABLE IF NOT EXISTS comments (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    post_id         BIGINT          NOT NULL,
    content         TEXT            NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP
);