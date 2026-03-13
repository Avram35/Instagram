CREATE TABLE IF NOT EXISTS follows (
    id              BIGSERIAL       PRIMARY KEY,
    follower_id     BIGINT          NOT NULL,
    following_id    BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    CONSTRAINT uk_follows_pair UNIQUE (follower_id, following_id)
);

CREATE TABLE IF NOT EXISTS follow_requests (
    id              BIGSERIAL       PRIMARY KEY,
    sender_id       BIGINT          NOT NULL,
    receiver_id     BIGINT          NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    CONSTRAINT uk_follow_requests_pair UNIQUE (sender_id, receiver_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL       PRIMARY KEY,
    recipient_id    BIGINT,
    sender_id       BIGINT,
    type            VARCHAR(50),
    post_id         BIGINT,
    read            BOOLEAN         DEFAULT FALSE,
    created_at      TIMESTAMP
);