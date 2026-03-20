SET NAMES 'utf8mb4';
SET time_zone = '+00:00';

START TRANSACTION;

-- Users
CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- UserWallets
CREATE TABLE IF NOT EXISTS UserWallets (
    user_id INT PRIMARY KEY,
    wallet_address VARCHAR(255) UNIQUE NOT NULL,
    wallet_private_key VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_userwallets_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @wallet_private_key_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'UserWallets'
      AND COLUMN_NAME = 'wallet_private_key'
);

SET @wallet_private_key_migration = IF(
    @wallet_private_key_exists = 0,
    'ALTER TABLE UserWallets ADD COLUMN wallet_private_key VARCHAR(255) NOT NULL DEFAULT '''' AFTER wallet_address',
    'SELECT ''wallet_private_key already exists'' AS migration_status'
);

PREPARE wallet_private_key_stmt FROM @wallet_private_key_migration;
EXECUTE wallet_private_key_stmt;
DEALLOCATE PREPARE wallet_private_key_stmt;

COMMIT;

/*
-- Categories
CREATE TABLE IF NOT EXISTS Categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    parent_id INT,

    CONSTRAINT fk_categories_categories
        FOREIGN KEY (parent_id) REFERENCES Categories(category_id)
        ON DELETE SET NULL
);

-- Items
CREATE TABLE IF NOT EXISTS Items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    price DECIMAL(30, 8), -- Price in crypto amount
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category_id INT,
    stock INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_items_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id),
    
    CONSTRAINT fk_items_categories
        FOREIGN KEY (category_id) REFERENCES Categories(category_id)
        ON DELETE SET NULL
);

-- ItemImages
CREATE TABLE IF NOT EXISTS ItemImages (
    image_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    alt_text TEXT,
    position INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_itemimages_items
        FOREIGN KEY (item_id) REFERENCES Items(item_id)
        ON DELETE CASCADE
);

-- Tags
CREATE TABLE IF NOT EXISTS Tags (
    tag_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    name VARCHAR(255) UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tags_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE SET NULL 
);

-- ItemTags
CREATE TABLE IF NOT EXISTS ItemTags (
    item_id INT,
    tag_id INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (item_id, tag_id),

    CONSTRAINT fk_itemtags_items
        FOREIGN KEY (item_id) REFERENCES Items(item_id)
        ON DELETE CASCADE,
    
    CONSTRAINT fk_itemtags_tags
        FOREIGN KEY (tag_id) REFERENCES Tags(tag_id)
        ON DELETE CASCADE
);

-- Orders
CREATE TABLE IF NOT EXISTS Orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    item_id INT,
    quantity INT DEFAULT 1,
    price DECIMAL(30, 8) NOT NULL, -- Price in crypto
    fee_percentage DECIMAL(5, 2) DEFAULT 2.5,
    order_status ENUM('pending', 'completed', 'cancelled') DEFAULT 'pending' NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_orders_items
        FOREIGN KEY (item_id) REFERENCES Items(item_id)
        ON DELETE SET NULL 
);

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

-- CryptoTransactions
CREATE TABLE IF NOT EXISTS CryptoTransactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,
    user_id INT,
    amount DECIMAL(30, 8), -- Amount in crypto
    transaction_type ENUM('payment', 'refund', 'fee', 'deposit', 'withdrawal', 'gift', 'adjustment', 'other') NOT NULL,
    transaction_hash VARCHAR(255) UNIQUE,
    transaction_status ENUM('pending', 'confirmed', 'failed') DEFAULT 'pending' NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    confirmed_at DATETIME,

    CONSTRAINT fk_cryptotransactions_orders
        FOREIGN KEY (order_id) REFERENCES Orders(order_id)
        ON DELETE CASCADE,
    
    CONSTRAINT fk_cryptotransactions_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);

-- UserWallets
CREATE TABLE IF NOT EXISTS UserWallets (
    user_id INT PRIMARY KEY,
    wallet_address VARCHAR(255) UNIQUE NOT NULL,
    wallet_private_key VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_userwallets_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);

-- Reviews
CREATE TABLE IF NOT EXISTS Reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    user_id INT NOT NULL,
    item_rating INT CHECK (item_rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_reviews_items
        FOREIGN KEY (item_id) REFERENCES Items(item_id)
        ON DELETE CASCADE,
    
    CONSTRAINT fk_reviews_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);

-- Notifications
CREATE TABLE IF NOT EXISTS Notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    notification_type ENUM('order', 'gift', 'review', 'system', 'other'),
    seen BOOLEAN DEFAULT FALSE,
    reference_id INT DEFAULT NULL,
    reference_type VARCHAR(255) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_notifications_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);
*/
