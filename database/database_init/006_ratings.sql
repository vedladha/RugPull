SET NAMES 'utf8mb4';
SET time_zone = '+00:00';

START TRANSACTION;

CREATE TABLE ratings (
    rating_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    user_id INT NOT NULL,
    order_id INT,
    rating_value TINYINT NOT NULL CHECK (rating_value BETWEEN 1 AND 5),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    UNIQUE (user_id, item_id),

    INDEX idx_ratings_order (order_id),

    CONSTRAINT fk_ratings_items
    FOREIGN KEY (item_id) REFERENCES items (item_id)
    ON DELETE CASCADE,

    CONSTRAINT fk_ratings_users
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE CASCADE
);

CREATE VIEW item_ratings_summary AS
SELECT
    item_id,
    COUNT(*) AS rating_count,
    AVG(rating_value) AS average_rating
FROM ratings
WHERE deleted = FALSE
GROUP BY item_id;

COMMIT;
