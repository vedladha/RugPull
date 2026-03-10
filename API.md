# Marketplace API Endpoints



This document outlines the main API endpoints for the $RPC marketplace.



---

## Overview

---


### 1. Users & Authentication

Handles signup, login, and account management.

Current frontend-backed auth flow uses `/api/auth/*` endpoints.
Legacy `/auth/*` auth routes still exist in a separate controller, but frontend signup/login/logout uses `/api/auth/*`.



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/signup` | POST | Create a new user and wallet (`walletAddress` returned in `user`) |
| `/api/auth/login` | POST | Authenticate user |
| `/api/auth/logout` | POST | Logout |
| `/auth/refresh` | POST | Refresh the JWT/session token |
| `/users/me` | GET | Fetch the logged in user account info |
| `/users/me` | PUT/PATCH | Update logged in user account info |
| `/users/me` | DELETE | Soft delete logged in user |



---



### 2. User Profiles

Manages display information for users.



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/profiles/{user_id}` | GET | Fetch profile info (`display_name`, `bio`) |
| `/profiles/me` | GET | Fetch profile info for current user |
| `/profiles/me` | PUT/PATCH | Update profile info for current user |



---



### 3. Items

Marketplace listings management.



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/items` | GET | List all items (filter by category, tag, price) |
| `/items/{item_id}` | GET | Fetch single item details |
| `/items` | POST | Create a new item listing |
| `/items/{item_id}` | PUT/PATCH | Update item info (price, stock, description) |
| `/items/{item_id}` | DELETE | Soft delete an item |
| `/items/{item_id}/images` | POST | Upload images for item |
| `/items/{item_id}/images/{image_id}` | DELETE | Remove item image |
| `/items/{item_id}/tags` | POST | Attach a tag to an item |
| `/items/{item_id}/tags/{tag_id}` | DELETE | Remove tag from item |



---



### 4. Categories



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/categories` | GET | List all categories |
| `/categories/{category_id}` | GET | Get category details (including parent/children) |
| `/categories` | POST | Create a new category |
| `/categories/{category_id}` | PUT/PATCH | Update category info |
| `/categories/{category_id}` | DELETE | Soft delete category |



---



### 5. Tags



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/tags` | GET | List all tags |
| `/tags/{tag_id}` | GET | Get tag info |
| `/tags` | POST | Create a new tag |
| `/tags/{tag_id}` | DELETE | Delete tag |



---



### 6. Orders

Buying and selling workflow.



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/orders` | GET | List user orders |
| `/orders/{order_id}` | GET | Get order details |
| `/orders` | POST | Place a new order |
| `/orders/{order_id}` | PUT/PATCH | Update order info (cancel, etc.) |



---



### 7. Wishlist



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/wishlist` | GET | List items in user’s wishlist |
| `/wishlist` | POST | Add item to wishlist |
| `/wishlist/{item_id}` | DELETE | Remove item from wishlist |



---



### 8. Crypto Transactions & Wallets



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/wallets` | GET | Fetch wallet balance |
| `/wallets/deposit` | POST | Deposit crypto to wallet |
| `/wallets/withdraw` | POST | Withdraw crypto from wallet |
| `/wallets/transfer` | POST | Transfer crypto from wallet to a different wallet |
| `/transactions` | GET | List user transactions |
| `/transactions/{transaction_id}` | GET | Get transaction details |



---



### 9. Reviews



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/reviews` | GET | List all reviews (filter by item/user) |
| `/reviews/{review_id}` | GET | Fetch single review |
| `/reviews` | POST | Create a new review for an item |
| `/reviews/{review_id}` | PUT/PATCH | Update review |
| `/reviews/{review_id}` | DELETE | Soft delete review |



---



### 10. Notifications



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/notifications` | GET | List notifications for user |
| `/notifications/{notification_id}` | GET | Fetch notification details |
| `/notifications/{notification_id}/read` | PATCH | Mark notification as read |



---


## Detailed Endpoint Reference

---

### POST /api/auth/signup

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | String | Y | User email |
| password | String | Y | Password |
| displayName | String | N | Optional user display name |


#### Request Example
```json
{
  "email": "user@example.com",
  "password": "password123",
  "displayName": "User"
}
```

#### Response Example
```json
{
  "user": {
    "id": 1,
    "email": "user@example.com",
    "displayName": "User",
    "walletAddress": "0.0.1234567"
  }
}
```

---

### POST /api/auth/login

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | String | Y | User email |
| password | String | Y | User password |

#### Request Example
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### Response Example
```json
{
  "user": {
    "id": 1,
    "email": "user@example.com",
    "displayName": "User",
    "walletAddress": "0.0.1234567"
  }
}
```

---

### POST /api/auth/logout

#### Response Example
```json
{
  "message": "Logged out"
}
```

---

### POST /auth/refresh

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| refresh_token | String | Y | The refresh token issued at login or signup |

#### Request Example
```json
{
  "refresh_token": "{jwt refresh token}"
}
```

#### Response Example
```json
{
  "access_token": "{jwt access token}",
  "expires_in": 3600
}
```

---

### GET /users/me

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Response Example
```json
{
  "id": 1,
  "email": "user@example.com",
  "display_name": "User",
  "bio": "A really cool user."
}
```

---

###  PUT/PATCH /users/me

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | String | N | New email |
| password | String | N | New password |

#### Request Example
```json
{
  "email": "newemail@example.com",
  "password": "NewPassword321",
}
```

---

#### Response Example
```json
{
  "id": 1,
  "email": "newemail@example.com",
}
```

---

### DELETE /users/me

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Response Example
```json
{
  "message": "User account marked as deleted",
  "deleted": true
}
```

---

### GET /profiles/{user_id}

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| user_id | String | Y | ID of the user whose profile to fetch |

#### Response Example
```json
{
  "id": 1,
  "display_name": "User",
  "bio": "A really cool user"
}
```

---

### GET /profiles/me

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Response Example
```json
{
  "id": 1,
  "display_name": "User",
  "bio": "A really cool user"
}
```

---

### PUT/PATCH /profiles/me

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| display_name | String | N | Update display name |
| bio | String | N | Update bio |

#### Request Example
```json
{
  "display_name": "NewUser",
  "bio": "New bio"
}
```

#### Response Example
```json
{
  "id": 1,
  "display_name": "NewUser",
  "bio": "New bio"
}
```

---

### GET /items

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| category | String | N | Filter items by category ID or name |
| tag | String | N | Filter items by tag ID or name |
| price_min | Number | N | Minimum price for filtering |
| price_max | Number | N | Maximum price for filtering |
| page | Number | N | Page number for pagination |
| limit | Number | N | Number of items per page |

#### Response Example
```json
{
  {
    "id": 1,
    "title": "Painting",
    "description": "Limited edition painting",
    "price": 2,
    "stock": 10,
    "category": "Art",
    "tags": ["Painting", "Art"],
    "images": ["image1.png", "image2.png"]
  },
  {
    "id": 2,
    "title": "Pokémon Card",
    "description": "Limited edition Pokémon card",
    "price": 5.12,
    "stock": 2,
    "category": "Collectibles",
    "tags": ["Pokémon", "Rare"],
    "images": []
  }
}
```

---

### GET /items/{item_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| item_id | String | Y | ID of the item |

#### Response Example
```json
{
  "id": 1,
  "title": "Painting",
  "description": "Limited edition painting",
  "price": 2,
  "stock": 10,
  "category": "Art",
  "tags": ["Painting", "Art"],
  "images": ["image1.png", "image2.png"]
}
```

---

### POST /items

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | String | Y | Item title |
| description | String | Y | Item description |
| price | Number | Y | Price in crypto |
| stock | Number | Y | Number of item available |
| category | String | Y | Category for the item |
| tags | Array | N | Array of tags |

#### Request Example
```json
{
  "title": "Cool Box",
  "description": "A super cool box",
  "price": 10000,
  "stock": 1,
  "category": "Trinkets",
  "tags": ["Box", "Awesome", "Cool"]
}
```

#### Response Example
```json
{
  "id": 3,
  "title": "Cool Box",
  "description": "A super cool box",
  "price": 10000,
  "stock": 2,
  "category": "Trinkets",
  "tags": ["Box", "Awesome", "Cool"]
}
```

---

### PUT/PATCH /items/{item_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| item_id | String | Y | ID of the item|

#### Request Body
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | String | N | New item title |
| description | String | N | New item description |
| price | Number | N | New item price |
| stock | Number | N | New item stock |
| category | String | N | New item category |
| tags | Array | N | New item tags |

#### Request Example
```json
{
  "price: 9999999,
  "stock": 1
}
```

#### Response Example
```json
{
  "id": 3,
  "title": "Cool Box",
  "description": "A super cool box",
  "price": 9999999,
  "stock": 1,
  "category": "Trinkets",
  "tags": ["Box", "Awesome", "Cool"]
}
```

---

### DELETE /items/{item_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| item_id | String | Y | ID of the item |

#### Response Example
```json
{
  "message": "Item successfully deleted",
  "deleted": true
}
```

---

### POST /items/{item_id}/images

#### Headers
| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |
| Content-Type | multipart/form-data |

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| item_id | String | Y | ID of the item |

#### Response Example
```json
{
  "uploaded": ["image1.png", "image2.png"]
}
```

---

### DELETE /items/{item_id}/images/{image_id}

#### Headers
| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| item_id | String | Y | ID of the item |
| image_id | String | Y | ID of the image |

#### Response Example
```json
{
  "message": "Image successfully removed"
}
```

---

### POST /items/{item_id}/tags

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| item_id | String | Y | ID of the item |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| tag | String | Y | Tag to attach |

#### Request Example
```json
{
  "tag": "Art"
}
```

#### Response Example
```json
{
  "message": "Tag attached successfully"
}
```

---

### DELETE /items/{item_id}/tags/{tag}

#### Headers
| Header | Value |
|--------|-------|
| Authorization | Bearer <access_token> |

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| item_id | String | Y | ID of the item |
| tag | String | Y | Tag to remove |

#### Response Example
```json
{
  "message": "Tag removed successfully"
}
```

---

### GET /categories

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| page | Number | N | Page number for pagination |
| limit | Number | N | Number of categories per page |

#### Response Example
```json
{
  {
    "id": 1,
    "name": "Art",
    "parent_id": null
  },
  {
    "id": 2,
    "name": "Digital Art",
    "parent_id": 1
  }
}
```

---

### GET /categories/{category_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| category_id | Number | Y | ID of the category to fetch |

#### Response Example
```json
{
  "id": 2,
  "name": "Digital Art",
  "parent_id": 1,
  "description": "Digital artwork"
}
```

---

### POST /categories

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Request Body
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Y | Name of the category |
| parent_id | Number | N | Parent category |
| description | String | N | Description |

#### Request Example
```json
{
  "name": "Sports",
  "description": "Physical activities"
}
```

#### Response Example
```json
{
  "id": 3,
  "name": "Sports",
  "parent_id": null,
  "description": "Physical activites"
}
```

---

### PUT/PATCH /categories/{category_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| category_id | Number | Y | ID of the category to update |

#### Request Body
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Y | New name |
| parent_id | Number | N | New parent category |
| description | String | N | New description |

#### Request Example
```json
{
  "name": "New Digital Art",
  "description": "New and improved digital artwork"
}
```

#### Response Example
```json
{
  "id": 2,
  "name": "New Digital Art",
  "parent_id": 1,
  "description": "New and improved digital artwork"
}
```

---

### DELETE /categories/{category_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| category_id | Number | Y | ID of the category to delete |

#### Response Example
```json
{
  "message": "Category deleted",
  "deleted": true
}
```

---

### GET /tags

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| page | Number | N | Page number for pagination |
| limit | Number | N | Number of categories per page |

#### Response Example
```json
[
  {
    "id": 1,
    "name": "Outside"
  },
  {
    "id": 2,
    "name": "Blue"
  }
]
```

---

### GET /tags/{tag_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| tag_id | Number | Y | ID of the tag |

#### Response Example
```json
{
  "id": 2,
  "name": "Blue"
}
```

---

### POST /tags

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Y | Name of the tag |

#### Request Example
```json
{
  "name": "Red"
}
```

#### Response Example
```json
{
  "id": 4,
  "name": "Red"
}
```

---

### DELETE /tags/{tag_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| tag_id | Number | Y | ID of the tag |

#### Response Example
```json
{
  "message": "Tag successfully deleted"
}
```

---

### GET /orders

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | String | N | Filter by status |
| page | Number | N | Page number for pagination |
| limit | Number | N | Number of orders per page |

#### Response Example
```json
[
  {
    "id": 5001,
    "user_id": 123,
    "status": "pending",
    "total_amount": 4.7,
    "created_at": "2026-02-18T17:00:00Z"
  },
  {
    "id": 5002,
    "user_id": 123,
    "status": "completed",
    "total_amount": 1.2,
    "created_at": "2026-02-17T14:30:00Z"
  }
]
```

---

### GET /orders/{order_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| order_id | Number | Y | ID of the order |

#### Response Example
```json
{
  "id": 5001,
  "user_id": 123,
  "status": "pending",
  "items": [
    {
      "item_id": 101,
      "title": "Crypto Art #1",
      "quantity": 1,
      "price": 2.5
    },
    {
      "item_id": 102,
      "title": "Crypto Collectible #2",
      "quantity": 2,
      "price": 1.1
    }
  ],
  "total_amount": 4.7,
  "created_at": "2026-02-18T17:00:00Z"
}
```

---

### POST /orders

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| items | array | Y | Array of items to purchase |

#### Items Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| item_id | Number | Y | ID of the item |
| quantity | Number | Y | Quantity to purchase |

#### Request Example
```json
{
  "items": [
    { "item_id": 100, "quantity": 1 },
    { "item_id": 234, "quantity": 3 }
  ]
}
```

#### Response Example
```json
{
  "id": 5003,
  "user_id": 1,
  "status": "pending",
  "total_amount": 67
}
```

---

### PUT/PATCH /orders/{order_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| order_id | Number | Y | ID of the order |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| status | String | Y | Update order status |

#### Request Example
```json
{
  "status": "cancelled"
}
```

#### Response Example
```json
{
  "id": 5003,
  "status": "cancelled"
}
```

---

### GET /widhlist

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| page | Number | N | Page number for pagination |
| limit | Number | N | Number of orders per page |

#### Response Example
```json
[
  {
    "item_id": 101,
    "title": "Crypto Art #1",
    "price": 2.5,
    "thumbnail": "image1.png",
    "added_at": "2026-02-18T19:00:00Z"
  },
  {
    "item_id": 205,
    "title": "Rare Collectible #7",
    "price": 5.0,
    "thumbnail": "image7.png",
    "added_at": "2026-02-17T15:22:00Z"
  }
]
```

---

### POST /wishlist

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| item_id | Number | Y | ID of the item to add |

#### Request Example
```json
{
  "item_id": 69
}
```

#### Response Example
```json
{
  "message": "Item added to wishlist",
  "item_id": 69
}
```

---

### DELETE /wishlist/{item_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-------|------|----------|-------------|
| item_id | Number | Y | ID of the item to remove |

#### Response Example
```json
{
  "message": "Item removed from wishlist",
  "item_id": 101
}
```

---

### GET /wallets

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Response Example
```json
{
  "user_id": 123,
  "balances": [
    {
      "currency": "RPC",
      "available": 0.75,
      "locked": 0.05
    }
  ],
  "updated_at": "2026-02-18T20:00:00Z"
}
```

---

### POST /wallets/deposit

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| amount | Number | Y | Amount to deposit |

#### Request Example
```json
{
  "amount": 1.2
}
```

#### Response Example
```json
{
  "message": "Deposit recorded and pending confirmation",
  "amount": 1.2,
  "status": "pending"
}
```

---

### POST /wallets/withdraw

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| amount | Number | Y | Amount to withdraw |

#### Request Example
```json
{
  "amount": 1.2
}
```

#### Response Example
```json
{
  "message": "Withdrawal recorded and pending confirmation",
  "amount": 1.2,
  "status": "pending"
}
```

---

### POST /wallets/transfer

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| amount | Number | Y | Amount to withdraw |
| recipient_user_id | Number | Y | ID of recipient user |

#### Request Example
```json
{
  "amount": 2,
  "recipient_user_id": 345
}
```

#### Response Example
```json
{
  "message": "Transfer completed",
  "amount": 2,
  "from_user_id": 1,
  "to_user_id": 345,
  "transaction_id": 3456
}
```

---

### GET /transactions

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameter | Type | Required | Description |
|-------|------|----------|-------------|
| type | String | N | Filter by type |
| status | String | N | Filter by status |
| page | Number | N | Page number for pagination |
| limit | Number | N | Number of transactions per page |

#### Response Example
```json
[
  {
    "id": 9001,
    "type": "withdraw",
    "amount": 0.25,
    "status": "pending",
    "created_at": "2026-02-18T20:15:00Z"
  },
  {
    "id": 9010,
    "type": "transfer",
    "amount": 1.5,
    "status": "completed",
    "created_at": "2026-02-18T20:20:00Z"
  }
]
```

---

### GET /transactions/{transaction_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-------|------|----------|-------------|
| transaction_id | Number | Y | ID of the transaction |

#### Response Example
```json
{
  "id": 9001,
  "type": "withdraw",
  "amount": 0.25,
  "status": "pending",
  "details": {
    "destination_addr": "1FfmbHfnpaZjKFvyi1okTjJJusN455paPH"
  },
  "created_at": "2026-02-18T20:15:00Z",
  "updated_at": "2026-02-18T20:16:00Z"
}
```

---

### GET /reviews

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameters | Type | Required | Description |
|------------|------|----------|-------------|
| item_id | Number | N | Filters reviews by item |
| user_id | Number | N | Filters reviews by user |
| rating | Number | N | Filters review by rating |
| page | Number | N | Page number for pagination |
| limit | Number | N | Number of reviews per page |

#### Response Example
```json
[
  {
    "id": 7001,
    "item_id": 101,
    "user_id": 123,
    "rating": 5,
    "comment": "Amazing quality!",
    "created_at": "2026-02-18T21:00:00Z"
  },
  {
    "id": 7002,
    "item_id": 101,
    "user_id": 456,
    "rating": 4,
    "comment": "Great but a bit pricey.",
    "created_at": "2026-02-18T21:05:00Z"
  }
]
```

---

### GET /reviews/{review_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| review_id | Number | Y | ID of the review |

#### Response Example
```json
{
  "id": 7001,
  "item_id": 101,
  "user_id": 123,
  "rating": 5,
  "comment": "Amazing quality!",
  "created_at": "2026-02-18T21:00:00Z",
  "updated_at": "2026-02-18T21:00:00Z"
}
```

---

### POST /reviews

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| item_id | Number | Y | ID of the item being reviewed |
| rating | Number | Y | Rating value |
| Comment | String | N | Review text |

#### Request Example
```json
{
  "item_id": 101,
  "rating": 5,
  "comment": "Absolutely loved it!"
}
```

#### Response Example
```json
{
  "id": 7003,
  "item_id": 101,
  "user_id": 123,
  "rating": 5,
  "comment": "Absolutely loved it!",
  "created_at": "2026-02-18T21:15:00Z"
}
```

---

### PUT/PATCH /reviews/{review_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| review_id | Number | Y | ID of the review |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| rating | Number | Y | New rating value |
| Comment | String | N | New review text |

#### Request Example
```json
{
  "rating": 4,
  "comment": "Great!"
}
```

#### Response Example
```json
{
  "id": 7003,
  "rating": 4,
  "comment": "Great!",
  "updated_at": "2026-02-18T21:30:00Z"
}
```

---

### DELETE /reviews/{review_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| review_id | Number | Y | ID of the review |

#### Response Example
```json
{
  "message": "Review successfully deleted",
  "deleted": true
}
```

---

### GET /notifications

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| unread | Boolean | N | Filter by unread |
| page | Number | N | Page number for pagination |
| limit | Number | N | Number of notifications per page |

#### Response Example
```json
[
  {
    "id": 8001,
    "type": "order_update",
    "title": "Order Completed",
    "message": "Your order #5001 has been completed.",
    "read": false,
    "created_at": "2026-02-18T22:00:00Z"
  },
  {
    "id": 8002,
    "type": "wishlist_price_drop",
    "title": "Price Drop Alert",
    "message": "An item in your wishlist has dropped in price.",
    "read": true,
    "created_at": "2026-02-18T21:30:00Z"
  }
]
```

---

### GET /notifications/{notification_id}

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| notification_id | Number | Y | ID of the notification |

#### Response Example
```json
{
  "id": 8001,
  "type": "order_update",
  "title": "Order Completed",
  "message": "Your order #5001 has been completed.",
  "read": false,
  "metadata": {
    "order_id": 5001
  },
  "created_at": "2026-02-18T22:00:00Z"
}
```

---

### PATCH /notifications/{notification_id}/read

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| notification_id | Number | Y | ID of the notification |


#### Response Example
```json
{
  "id": 8001,
  "read": true,
  "updated_at": "2026-02-18T22:10:00Z"
}
```
