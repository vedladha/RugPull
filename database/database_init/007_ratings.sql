SET NAMES 'utf8mb4';
SET time_zone = '+00:00';

START TRANSACTION;

CREATE TABLE ratings (
    rating_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    item_id INT NOT NULL,
    rating_value TINYINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    UNIQUE (user_id, item_id),

    CONSTRAINT fk_ratings_users
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE CASCADE,

    CONSTRAINT fk_ratings_items
    FOREIGN KEY (item_id) REFERENCES items (item_id)
    ON DELETE CASCADE
);

COMMIT;
