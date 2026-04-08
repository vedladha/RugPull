SET NAMES 'utf8mb4';
SET time_zone = '+00:00';

START TRANSACTION;

CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    order_status ENUM(
        'PENDING', 'AWAITING_CONFIRMATION', 'COMPLETED', 'CANCELLED', 'FAILED'
    )
    DEFAULT 'pending'
    NOT NULL,
    total_price DECIMAL(30, 8) NOT NULL, -- Total price of all items in crypto
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_users
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,
    item_id INT,
    quantity INT DEFAULT 1,
    unit_price DECIMAL(30, 8) NOT NULL, -- Price in crypto

    CONSTRAINT fk_orderitems_orders
    FOREIGN KEY (order_id) REFERENCES orders (order_id)
    ON DELETE CASCADE,

    CONSTRAINT fk_orderitems_items
    FOREIGN KEY (item_id) REFERENCES items (item_id)
    ON DELETE SET NULL
);

COMMIT;
