SET NAMES 'utf8mb4';
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS daily_rewards (
    user_id INT,
    claimed_last DATETIME,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
