CREATE TABLE IF NOT EXISTS profiles (
    user_id             BIGSERIAL PRIMARY KEY,
    username            VARCHAR(30)    UNIQUE,
    fname               VARCHAR(50),
    lname               VARCHAR(50),
    bio                 VARCHAR(150),
    profile_picture_url VARCHAR(255),
    is_private     BOOLEAN         DEFAULT FALSE
);