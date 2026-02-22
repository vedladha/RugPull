CREATE DATABASE IF NOT EXISTS marketplace_dev;

USE marketplace_dev;

-- Users
CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)

-- UserProfiles
CREATE TABLE UserProfiles (
    user_id INT PRIMARY KEY,
    display_name VARCHAR(255) UNIQUE,
    bio TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_userprofiles_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
)

-- Items
CREATE TABLE Items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    price DECIMAL(30, 8), -- Price in crypto amount
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category_id INT,
    stock INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_items_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE SET NULL
    
    CONSTRAINT fk_items_categories
        FOREIGN KEY (category_id) REFERENCES Categories(category_id)
        ON DELETE SET NULL
)
-- When a user is deleted, soft delete all their items
DELIMITER $$

CREATE TRIGGER soft_delete_user_items
AFTER DELETE ON Users
FOR EACH ROW
BEGIN
    UPDATE Items
    SET deleted = TRUE
    WHERE user_id = OLD.user_id;
END $$

DELIMITER ;

-- ItemImages
CREATE TABLE ItemImages (
    image_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT,
    image_url VARCHAR NOT NULL,
    alt_text TEXT,
    position INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)