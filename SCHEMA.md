# Marketplace Database Schema

---

Implemented tables reflect the current database initialization scripts in `database/database_init/`.
Some older planned tables remain documented below, and those sections are called out explicitly.

## Users
**Stores authentication information for each user**

| Column          | Type                     | Notes                                      |
|-----------------|-------------------------|-------------------------------------------|
| user_id         | INT, primary key, auto-increment | Unique user identifier                  |
| email           | VARCHAR, unique, not NULL | Login email                               |
| password_hash   | VARCHAR, not NULL        | Hashed password                            |
| status          | ENUM('PENDING', 'ACTIVE', 'FAILED'), not NULL, default 'PENDING' | Tells status of associated wallet creation |
| created_at      | DATETIME, default current time |                                         |
| updated_at      | DATETIME, default current time |                                         |
| deleted         | BOOLEAN, default FALSE | Soft deletion for users |

---

## UserProfiles
**Stores display information for a user**

| Column        | Type                     | Notes                                      |
|---------------|-------------------------|-------------------------------------------|
| user_id       | INT, primary key, foreign key → Users.user_id | Unique connection to Users table |
| display_name  | VARCHAR                  | Username                                   |
| bio           | TEXT                     | User bio                                   |
| created_at    | DATETIME, default current time |                                         |
| updated_at    | DATETIME, default current time |                                         |

---

## Items
**Stores items on the marketplace**

| Column        | Type                     | Notes                                      |
|---------------|-------------------------|-------------------------------------------|
| item_id       | INT, primary key, auto-increment | Unique item identifier                  |
| user_id       | INT, foreign key → Users.user_id | Item seller                               |
| price         | DECIMAL(30,8)            | Price of the item in crypto amount        |
| name          | VARCHAR, not NULL        | Item name                                  |
| description   | TEXT, not NULL           | Item description                           |
| category_id   | INT, foreign key → Categories.category_id | Category of item                |
| stock         | INT, default 0           | Amount of item available for sale         |
| created_at    | DATETIME, default current time |                                         |
| updated_at    | DATETIME, default current time |                                         |
| deleted       | BOOLEAN, default FALSE   | Soft delete flag                           |

---

## ItemImages
**Stores user uploaded images for items**

| Column      | Type                     | Notes                                  |
|------------|-------------------------|---------------------------------------|
| image_id    | INT, primary key, auto-increment | Unique image identifier           |
| user_id     | INT, foreign key → Users.user_id | Uploader |
| item_id     | INT, foreign key → Items.item_id | Corresponding item                 |
| image_url   | VARCHAR, not NULL        | Link to the image                     |
| alt_text    | TEXT                     | Alt text for the image                |
| position    | INT, default 0           | Ordering for an item’s images         |
| created_at  | DATETIME, default current time |                                     |
| updated_at  | DATETIME, default current time |                                     |

---

## Categories
**Stores predetermined categories that items go under**

This table is not currently created by the active database initialization scripts.

| Column      | Type                     | Notes                                  |
|------------|-------------------------|---------------------------------------|
| category_id | INT, primary key, auto-increment | Unique category identifier        |
| name        | VARCHAR, unique          | Category name                          |
| parent_id   | INT, foreign key → Categories.category_id | Nested categories connect to a parent |

---

## Tags
**Stores user defined tags for an item**

This table is not currently created by the active database initialization scripts.

| Column      | Type                     | Notes                                  |
|------------|-------------------------|---------------------------------------|
| tag_id      | INT, primary key         | Unique tag identifier                  |
| user_id     | INT, foreign key → Users.user_id | User who created the tag           |
| name        | VARCHAR, unique          | Tag name                               |
| created_at  | DATETIME, default current time |                                     |

---

## ItemTags
**Stores connections between tags and items**

This table is not currently created by the active database initialization scripts.

| Column      | Type                     | Notes                                  |
|------------|-------------------------|---------------------------------------|
| item_id     | INT, foreign key → Items.item_id | Item to connect the tag to         |
| tag_id      | INT, foreign key → Tags.tag_id | Tag to connect to the item           |
| created_at  | DATETIME, default current time |                                     |

**Primary Key:** `(item_id, tag_id)`

---

## Orders
**Stores placed orders**

| Column         | Type                     | Notes                                  |
|----------------|-------------------------|---------------------------------------|
| order_id       | INT, primary key, auto-increment | Unique order identifier             |
| user_id        | INT, foreign key → Users.user_id | User who placed the order           |
| order_status   | ENUM('PENDING', 'AWAITING_CONFIRMATION', 'COMPLETED', 'CANCELLED', 'FAILED'), default 'PENDING' | Status of order completion |
| created_at     | DATETIME, default current timestamp |                                 |
| updated_at     | DATETIME, default current timestamp |                                 |

---

### Order Items
**Stores items within an order**

| Column | Type | Notes |
|--------|------|-------|
| order_item_id | INT, primary key, auto-increment | Unique order item identifier |
| order_id | INT foreign key → orders.order_id | Order this item is a part of |
| item_id | INT foreign key → items.item_id | Item being ordered |
| quantity | INT, default 1 | Number of this item being ordered |
| unit_price | DECIMAL(30, 8), not NULL | Price of one unit of the item in crypto |

## Cart
**Stores list of items for checkout**
| Column | Type | Notes |
|--------|------|-------|
| cart_id | INT, primary key, auto-increment | Unique cart item identifier |
| user_id | INT, foreign key → Users.user_id | User who has this item in their cart |
| item_id | INT, foreign key → Items.item_id | Item in the cart |
| quantity | INT, default 1 | Number of item in the cart |
| created_at | DATETIME, default current timestamp |

---

## Wishlist
**Stores list of wanted items for users**

| Column      | Type                     | Notes                                  |
|------------|-------------------------|---------------------------------------|
| user_id     | INT, foreign key → Users.user_id | User who has this item on wishlist |
| item_id     | INT, foreign key → Items.item_id | Item being wished for               |
| created_at  | DATETIME, default current timestamp |                                   |

**Primary Key:** `(user_id, item_id)`

---

## CryptoTransactions
**Stores all crypto transactions that happen in the marketplace**

| Column             | Type                     | Notes                                  |
|--------------------|-------------------------|---------------------------------------|
| transaction_id     | INT, primary key, auto-increment | Unique transaction identifier      |
| order_id           | INT, foreign key → Orders.order_id, optional | Optional connection to an item order |
| user_id            | INT, foreign key → Users.user_id | User engaged in transaction        |
| amount             | DECIMAL(30,8)            | Amount of crypto in the transaction   |
| transaction_type   | ENUM('payment','refund','fee','deposit','withdrawal','gift','adjustment') | Transaction type |
| transaction_hash   | VARCHAR, unique          | Transaction hash on blockchain        |
| transaction_status | ENUM('pending','confirmed','failed') | Transaction status                 |
| created_at         | DATETIME, default current timestamp |                                   |
| confirmed_at       | DATETIME, nullable       | Time transaction was confirmed        |

---

## UserWallets
**Stores user wallet information**

| Column        | Type                     | Notes                                  |
|---------------|-------------------------|---------------------------------------|
| user_id       | INT, primary key, foreign key → Users.user_id | Unique user identifier         |
| wallet_address| VARCHAR(255) | Wallet to retrieve balance from       |
| wallet_private_key | VARCHAR(255), not NULL | Wallet private key (currently stored as plain text) |
| created_at    | DATETIME, default current time |                                     |
| updated_at    | DATETIME, default current time |                                     |

---

## DailyRewards
**Stores daily reward streak and last-claim time for each user**

| Column        | Type                     | Notes                                  |
|---------------|-------------------------|---------------------------------------|
| user_id       | INT, primary key, foreign key → Users.user_id | Unique user identifier |
| streak_length | INT                      | Current daily reward streak           |
| claimed_last  | DATETIME                 | Time the reward was last claimed      |

---

## AdSessions
**Stores secure server-side advertisement sessions**

| Column          | Type                     | Notes                                  |
|-----------------|-------------------------|---------------------------------------|
| id              | VARCHAR(36), primary key | Session identifier                    |
| user_id         | INT, foreign key → Users.user_id | User watching the ad            |
| ad_title        | VARCHAR(100), not NULL   | Title of the selected ad              |
| required_duration_seconds | INT, not NULL | Required watch time                   |
| started_at      | DATETIME, not NULL       | Session start time                    |
| is_claimed      | BOOLEAN, default FALSE   | Whether the reward was claimed        |

---

## Ratings
**Stores 1-5 star ratings for items**

| Column        | Type                     | Notes                                  |
|---------------|-------------------------|---------------------------------------|
| rating_id     | INT, primary key, auto-increment | Unique rating identifier         |
| user_id       | INT, foreign key → Users.user_id | User rating the item              |
| item_id       | INT, foreign key → Items.item_id | Item being rated                  |
| rating_value  | TINYINT, not NULL        | Rating value from 1 to 5               |
| created_at    | DATETIME, default current time |                                     |
| updated_at    | DATETIME, default current time |                                     |
| deleted       | BOOLEAN, default FALSE   | Soft delete flag                       |

---

## Notifications
**Stores all notifications to deliver to users**

This table is not currently created by the active database initialization scripts.

---

## Relationships / Cardinality

**1:1 (One-to-One)**
- `Users.user_id → UserProfiles.user_id`
- `Users.user_id → UserWallets.user_id`
- `Users.user_id → DailyRewards.user_id`

**1:N (One-to-Many)**
- `Users.user_id → Items.user_id`
- `Users.user_id → Orders.user_id`
- `Users.user_id → Notifications.user_id`
- `Users.user_id → AdSessions.user_id`
- `Users.user_id → Ratings.user_id`
- `Items.item_id → ItemImages.item_id`
- `Items.item_id → Ratings.item_id`
- `Categories.category_id → Items.category_id`
- `Orders.order_id → CryptoTransactions.order_id (optional)`
- `Users.user_id → Tags.user_id`
- `Users.user_id → CryptoTransactions.user_id`

**N:M (Many-to-Many)**
- `Items.item_id ↔ Tags.tag_id via ItemTags`
- `Users.user_id ↔ Items.item_id via Wishlist`
