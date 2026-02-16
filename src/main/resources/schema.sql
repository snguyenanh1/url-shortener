    CREATE TABLE IF NOT EXISTS urls (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        url VARCHAR(255) NOT NULL,
        short_code VARCHAR(10) NOT NULL UNIQUE,
        created_at TIMESTAMP,
        expires_at TIMESTAMP
    );