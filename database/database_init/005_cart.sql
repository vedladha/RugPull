SET NAMES 'utf8mb4';
SET time_zone = '+00:00';

START TRANSACTION;

CREATE TABLE Cart (
    cart_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    item_id INT NOT NULL,
    quantity INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (user_id, item_id),

    CONSTRAINT fk_cart_users
        FOREIGN KEY(user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_cart_items
        FOREIGN KEY(item_id) REFERENCES Items(item_id)
        ON DELETE CASCADE
);

COMMIT;
