CREATE TABLE IF NOT EXISTS url
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    url        VARCHAR(255) NOT NULL,
    short_code VARCHAR(10)  NOT NULL UNIQUE,
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_by BIGINT,
    clicks BIGINT,
    CONSTRAINT fk_url_user FOREIGN KEY (created_by) REFERENCES user (id)
);

CREATE TABLE IF NOT EXISTS user
(
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255)           NOT NULL UNIQUE,
    password VARCHAR(255)           NOT NULL,
    email    VARCHAR(255)           NOT NULL UNIQUE,
    name     VARCHAR(255)           NOT NULL,
    role     ENUM ('USER', 'ADMIN') NOT NULL
)
