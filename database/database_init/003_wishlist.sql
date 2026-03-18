SET NAMES 'utf8mb4';
SET time_zone = '+00:00';

START TRANSACTION;


-- Wishlists
CREATE TABLE IF NOT EXISTS Wishlists (
    user_id INT,
    item_id INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, item_id),

    CONSTRAINT fk_wishlists_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_wishlists_items
        FOREIGN KEY (item_id) REFERENCES Items(item_id)
        ON DELETE CASCADE
);

COMMIT;
