SET NAMES 'utf8mb4';
SET time_zone = '+00:00';

START TRANSACTION;

CREATE TABLE item_images (
    image_id INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
    user_id INT NOT NULL,
    item_id INT NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    alt_text TEXT NOT NULL,
    `position` INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_itemimages_items
    FOREIGN KEY (item_id) REFERENCES items (item_id)
    ON DELETE CASCADE,

    CONSTRAINT fk_itemimages_users
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE CASCADE
);

COMMIT;
