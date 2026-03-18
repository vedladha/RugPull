SET NAMES 'utf8mb4';
SET time_zone = '+00:00';

START TRANSACTION;

CREATE TABLE ItemImages (
    image_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    item_id INT,
    image_url VARCHAR(2048),
    alt_text TEXT,
    position INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_itemimages_items
        FOREIGN KEY (item_id) REFERENCES Items(item_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_itemimages_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
)

COMMIT;