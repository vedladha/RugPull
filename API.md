# Marketplace API Endpoints



This document outlines the main API endpoints for the crypto-powered marketplace.



---

## Overview

---


### 1. Users & Authentication

Handles signup, login, and account management.



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/auth/signup` | POST | Create a new user (email, password, optional profile info) |
| `/auth/login` | POST | Authenticate user and return JWT/session token |
| `/auth/logout` | POST | Invalidate token/session |
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
| `/orders/{order_id}/status` | PATCH | Update order status (`pending`, `completed`, `cancelled`) |



---



### 7. Wishlist



| Endpoint | Method | Description |
|----------|--------|-------------|
| `/wishlist` | GET | List items in user’s wishlist |
| `/wishlist` | POST | Add item to wishlist |
| `/wishlist/{item_id}` | DELETE | Remove item from wishlist |



---



### 8. Crypto Transactions \& Wallets



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

### POST /auth/signup

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | String | Y | User email |
| password | String | Y | Password |
| display_name | String | N | Optional user display name, defaults to email |


#### Request Example
```json
{
  "email": "user@example.com",
  "password": "password123",
  "display_name": "User"
}
```

#### Response Example
```json
{
  "user": {
    "id": 1,
    "email": "user@example.com",
    "display_name": "User"
  },
  "access_token": "{jwt access token}",
  "refresh_token": "{jwt refresh token}",
  "expires_in": 3600
}
```

---

### POST /auth/login

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
    "display_name": "User"
  },
  "access_token": "{jwt access token}",
  "refresh_token": "{jwt refresh token}",
  "expires_in": 3600
}
```

---

### POST /auth/logout

#### Headers

| Header | Value |
|--------|-------|
| Authorization | Bearer `<access_token>` |

#### Response Example
```json
{
  "message": "User successfully logged out"
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

