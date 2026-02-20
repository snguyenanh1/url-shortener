DROP TABLE IF EXISTS url;
DROP TABLE IF EXISTS user;

CREATE TABLE user
(
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255)           NOT NULL UNIQUE,
    password VARCHAR(255)           NOT NULL,
    email    VARCHAR(255)           NOT NULL UNIQUE,
    name     VARCHAR(255)           NOT NULL,
    role     ENUM ('USER', 'ADMIN') NOT NULL
);

CREATE TABLE url
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    url        VARCHAR(255) NOT NULL,
    short_code VARCHAR(10)  NOT NULL UNIQUE,
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_by BIGINT,
    clicks BIGINT,
    CONSTRAINT fk_url_user FOREIGN KEY (created_by) REFERENCES user (id) ON DELETE SET NULL
);
