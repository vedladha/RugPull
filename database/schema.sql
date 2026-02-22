CREATE DATABASE IF NOT EXISTS marketplace_dev;

USE marketplace_dev;

-- Users
CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

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
);

-- Categories
CREATE TABLE Categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    parent_id INT,

    CONSTRAINT fk_categories_categories
        FOREIGN KEY (parent_id) REFERENCES Categories(category_id)
        ON DELETE SET NULL
);

-- Items
CREATE TABLE Items (
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
CREATE TABLE ItemImages (
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
CREATE TABLE Tags (
    tag_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    name VARCHAR(255) UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tags_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE SET NULL 
);

-- ItemTags
CREATE TABLE ItemTags (
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
CREATE TABLE Orders (
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
CREATE TABLE WISHLISTS (
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
CREATE TABLE CryptoTransactions (
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
CREATE TABLE UserWallets (
    user_id INT PRIMARY KEY,
    wallet_address VARCHAR(255) UNIQUE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_userwallets_users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);

-- Reviews
CREATE TABLE Reviews (
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
CREATE TABLE Notifications (
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

CREATE INDEX idx_items_user ON Items(user_id);
CREATE INDEX idx_orders_user ON Orders(user_id);
CREATE INDEX idx_orders_item ON Orders(item_id);
CREATE INDEX idx_reviews_item ON Reviews(item_id);
CREATE INDEX idx_reviews_user ON Reviews(user_id);
CREATE INDEX idx_items_category ON Items(category_id);
CREATE INDEX idx_itemimages_item ON ItemImages(item_id);
CREATE INDEX idx_cryptotransactions_user ON CryptoTransactions(user_id);
CREATE INDEX idx_cryptotransactions_order ON CryptoTransactions(order_id);
CREATE INDEX idx_notifications_user ON Notifications(user_id);
CREATE INDEX idx_items_user_deleted ON Items(user_id, deleted);
CREATE INDEX idx_users_deleted ON Users(deleted);
CREATE INDEX idx_orders_status ON Orders(order_status);
CREATE INDEX idx_cryptotransactions_status ON CryptoTransactions(transaction_status);