SET NAMES 'utf8mb4';
SET time_zone = '+00:00';

START TRANSACTION;

-- Items
CREATE TABLE items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    price DECIMAL(30, 8), -- Price in crypto amount
    `Name` VARCHAR(255) NOT NULL,
    `Description` TEXT NOT NULL,
    -- category_id INT, Will be added later with categories
    stock INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_items_users
    FOREIGN KEY (user_id) REFERENCES users (user_id)
);

COMMIT;
