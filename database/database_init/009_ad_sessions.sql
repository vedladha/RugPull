SET NAMES 'utf8mb4';
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS ad_sessions (
    id VARCHAR(36) NOT NULL,
    user_id INT NOT NULL,
    ad_title VARCHAR(100) NOT NULL,
    required_duration_seconds INT NOT NULL,
    started_at DATETIME NOT NULL,
    is_claimed TINYINT(1) DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
