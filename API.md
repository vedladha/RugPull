# Marketplace API Endpoints

This document outlines the main API endpoints for the $RPC marketplace.



---

## Overview

---

### 1. Users & Authentication

Handles signup, login, logout, and authenticated user lookup.

Current frontend-backed auth flow uses `/auth/*` endpoints.
Successful login sets an HTTP-only `jwt` cookie.
Legacy `/api/users*` routes still exist in a separate controller, but they are not the primary
frontend-backed API.

| Endpoint         | Method | Description                                |
|------------------|--------|--------------------------------------------|
| `/auth/register` | POST   | Create a new user, profile, and wallet     |
| `/auth/login`    | POST   | Authenticate user and set the `jwt` cookie |
| `/auth/profile`  | GET    | Fetch the logged in user's auth profile    |
| `/auth/logout`   | POST   | Logout and clear the `jwt` cookie          |

---

### 2. User Profiles

Manages display information for users.

| Endpoint            | Method    | Description                               |
|---------------------|-----------|-------------------------------------------|
| `/profile/{userId}` | GET       | Fetch profile info (`displayName`, `bio`) |
| `/profile/me`       | GET       | Fetch profile info for current user       |
| `/profile/me`       | PUT/PATCH | Update profile info for current user      |

---

### 3. Items

Marketplace listings management.

| Endpoint          | Method | Description                                        |
|-------------------|--------|----------------------------------------------------|
| `/items`          | GET    | List all non-deleted items                         |
| `/items/{itemId}` | GET    | Fetch single item details                          |
| `/items`          | POST   | Create a new item listing                          |
| `/items/{itemId}` | PUT    | Update item info (name, description, price, stock) |
| `/items/{itemId}` | DELETE | Soft delete an item                                |
| `/items/batch`    | POST   | Fetch items using a bulk list of ids               |

---

### 4. Categories

No category endpoints are currently implemented in the backend controllers.



---

### 5. Tags

No tag endpoints are currently implemented in the backend controllers.



---

### 6. Orders

Buying and selling workflow.

| Endpoint            | Method | Description                        |
|---------------------|--------|------------------------------------|
| `/orders`           | GET    | List authenticated user's orders   |
| `/orders/{orderId}` | GET    | Get one authenticated user's order |
| `/orders`           | POST   | Place a new order                  |

---

### 7. Wishlist

| Endpoint             | Method | Description                                     |
|----------------------|--------|-------------------------------------------------|
| `/wishlist`          | GET    | List items in the authenticated user's wishlist |
| `/wishlist/{itemId}` | POST   | Add item to wishlist                            |
| `/wishlist/{itemId}` | DELETE | Remove item from wishlist                       |

---

### 8. Crypto Transactions & Wallets

| Endpoint   | Method | Description                               |
|------------|--------|-------------------------------------------|
| `/wallets` | GET    | Fetch authenticated user's wallet balance |

Deposit, withdraw, transfer, and transaction history endpoints are not currently implemented in
the backend controllers.



---

### 9. Reviews

No review endpoints are currently implemented in the backend controllers.



---

### 10. Notifications

No notification endpoints are currently implemented in the backend controllers.

---

### 11. Cart

| Endpoint         | Method | Description                                 |
|------------------|--------|---------------------------------------------|
| `/cart`          | GET    | List items in the authenticated user's cart |
| `/cart/{itemId}` | POST   | Add item to cart                            |
| `/cart/{itemId}` | PUT    | Update item quantity in cart                |
| `/cart/{itemId}` | DELETE | Remove item from cart                       |

---

### 12. Legacy `/api` User Endpoints

These routes still exist in `UserController`, but they are older/testing endpoints.

| Endpoint                      | Method | Description                                      |
|-------------------------------|--------|--------------------------------------------------|
| `/api/users`                  | GET    | Return all users as raw `User` entities          |
| `/api/users/addUser`          | POST   | Create a user from query parameters              |
| `/api/users/{userId}/profile` | PUT    | Create or update a profile from query parameters |
| `/api/users/{userId}/profile` | GET    | Return a raw `UserProfile` entity                |

---

## Detailed Endpoint Reference

---

Protected endpoints now use the HTTP-only `jwt` cookie set by `POST /auth/login`.
Older `Authorization: Bearer` references in untouched legacy sections below are not authoritative.

### POST /auth/register

#### Request Body

| Field       | Type   | Required | Description                |
|-------------|--------|----------|----------------------------|
| displayName | String | N        | Optional user display name |
| email       | String | Y        | User email                 |
| password    | String | Y        | Password                   |

#### Request Example

```json
{
  "displayName": "User",
  "email": "user@example.com",
  "password": "password123"
}
```

#### Response Example

```json
{
  "email": "user@example.com",
  "displayName": "User"
}
```

---

### POST /auth/login

#### Request Body

| Field    | Type   | Required | Description   |
|----------|--------|----------|---------------|
| email    | String | Y        | User email    |
| password | String | Y        | User password |

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
  "email": "user@example.com",
  "displayName": "User"
}
```

---

### POST /auth/logout

#### Response Example

```json
{
  "message": "Logged out successfully"
}
```

---

### GET /auth/profile

Returns the authenticated user's basic auth profile.

#### Response Example

```json
{
  "user": {
    "email": "user@example.com",
    "displayName": "User"
  }
}
```

---

### GET /users/me

This route is not implemented in the current backend.
Use `/auth/profile` for auth details or `/profile/me` for the full profile.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
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

### PUT/PATCH /users/me

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field    | Type   | Required | Description  |
|----------|--------|----------|--------------|
| email    | String | N        | New email    |
| password | String | N        | New password |

#### Request Example

```json
{
  "email": "newemail@example.com",
  "password": "NewPassword321"
}
```

---

#### Response Example

```json
{
  "id": 1,
  "email": "newemail@example.com"
}
```

---

### DELETE /users/me

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Response Example

```json
{
  "message": "User account marked as deleted",
  "deleted": true
}
```

---

### GET /profile/{userId}

#### Path Parameters

| Parameter | Type   | Required | Description                           |
|-----------|--------|----------|---------------------------------------|
| userId    | String | Y        | ID of the user whose profile to fetch |

#### Response Example

```json
{
  "profile": {
    "userId": 1,
    "displayName": "User",
    "bio": "A really cool user"
  }
}
```

---

### GET /profile/me

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Response Example

```json
{
  "profile": {
    "userId": 1,
    "displayName": "User",
    "bio": "A really cool user"
  }
}
```

---

### PUT/PATCH /profile/me

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field       | Type   | Required | Description         |
|-------------|--------|----------|---------------------|
| displayName | String | N        | Update display name |
| bio         | String | N        | Update bio          |

#### Request Example

```json
{
  "displayName": "NewUser",
  "bio": "New bio"
}
```

#### Response Example

```json
{
  "profile": {
    "userId": 1,
    "displayName": "NewUser",
    "bio": "New bio"
  }
}
```

---

### GET /items

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

Returns all non-deleted items.
The filtering query parameters listed below were part of an older draft and are not currently
implemented in the backend controller.

#### Response Example

```json
{
  "items": [
    {
      "itemId": 1,
      "name": "Painting",
      "description": "Limited edition painting",
      "price": 2.0,
      "stock": 10,
      "sellerName": "artist1"
    },
    {
      "itemId": 2,
      "name": "Pokemon Card",
      "description": "Limited edition Pokemon card",
      "price": 5.12,
      "stock": 2,
      "sellerName": "collector7"
    }
  ]
}
```

---

### GET /items/{itemId}

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description    |
|-----------|--------|----------|----------------|
| itemId    | String | Y        | ID of the item |

#### Response Example

```json
{
  "item": {
    "itemId": 1,
    "userId": 7,
    "name": "Painting",
    "description": "Limited edition painting",
    "price": 2.0,
    "stock": 10,
    "deleted": false
  }
}
```

---

### POST /items

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field       | Type   | Required | Description               |
|-------------|--------|----------|---------------------------|
| name        | String | Y        | Item name                 |
| description | String | Y        | Item description          |
| price       | Number | Y        | Price in crypto           |
| stock       | Number | Y        | Number of items available |

#### Request Example

```json
{
  "name": "Cool Box",
  "description": "A super cool box",
  "price": 10000,
  "stock": 1
}
```

#### Response Example

```json
{
  "item": {
    "itemId": 3,
    "userId": 34,
    "name": "Cool Box",
    "description": "A super cool box",
    "price": 10000,
    "stock": 1,
    "deleted": false
  }
}
```

---

### PUT /items/{itemId}

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description    |
|-----------|--------|----------|----------------|
| itemId    | String | Y        | ID of the item |

#### Request Body

| Field       | Type   | Required | Description                          |
|-------------|--------|----------|--------------------------------------|
| name        | String | Y        | Updated item name (non-empty)        |
| description | String | Y        | Updated item description (non-empty) |
| price       | Number | Y        | Updated price (must be non-negative) |
| stock       | Number | Y        | Updated stock (must be non-negative) |

#### Request Example

```json
{
  "name": "Updated Name",
  "description": "Updated Desc",
  "price": 25.50,
  "stock": 9
}
```

#### Response Example

```json
{
  "item": {
    "itemId": 3,
    "userId": 34,
    "name": "Updated Name",
    "description": "Updated Desc",
    "price": 25.50,
    "stock": 9,
    "deleted": false
  }
}
```

#### Error Responses

```json
{
  "error": "Item not found"
}
```

```json
{
  "error": "price must be non-negative"
}
```

---

### DELETE /items/{itemId}

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description    |
|-----------|--------|----------|----------------|
| itemId    | String | Y        | ID of the item |

#### Response Example

```json
{
  "message": "Item deleted",
  "itemId": 3
}
```

#### Error Response

```json
{
  "error": "Item not found"
}
```

### POST /items/batch

Retrieves a list of items specified by an array of IDs provided in the request body. Only active (non-deleted) items are returned in the response.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |
| Content-Type  | `application/json`      |

#### Request Body

A JSON array of integers representing the item IDs.

```json
[1, 2, 3]
```

#### Response Example

The response returns a json array items with item objects inside.

```json
{
  "items": [
    {
      "itemId": 1,
      "userId": 7,
      "name": "listing!",
      "description": "list!",
      "price": 1.00000000,
      "stock": 1,
      "createdAt": "2026-04-04T17:19:06",
      "updatedAt": "2026-04-04T17:19:06",
      "deleted": false
    },
    {
      "itemId": 2,
      "userId": 12,
      "name": "Second Item",
      "description": "Another description",
      "price": 15.50,
      "stock": 5,
      "createdAt": "2026-04-04T18:00:00",
      "updatedAt": "2026-04-04T18:00:00",
      "deleted": false
    }
  ]
}
```

#### Error Responses

Returned if the request body is null, empty, or not a valid JSON array of integers.

```json
{
  "error": "request body is empty or missing"
}
```

---

---

### POST /items/{item_id}/images

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |
| Content-Type  | multipart/form-data     |

#### Path Parameters

| Parameter | Type   | Required | Description    |
|-----------|--------|----------|----------------|
| item_id   | String | Y        | ID of the item |

#### Response Example

```json
{
  "uploaded": [
    "image1.png",
    "image2.png"
  ]
}
```

---

### DELETE /items/{item_id}/images/{image_id}

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description     |
|-----------|--------|----------|-----------------|
| item_id   | String | Y        | ID of the item  |
| image_id  | String | Y        | ID of the image |

#### Response Example

```json
{
  "message": "Image successfully removed"
}
```

---

### POST /items/{item_id}/tags

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description    |
|-----------|--------|----------|----------------|
| item_id   | String | Y        | ID of the item |

#### Request Body

| Field | Type   | Required | Description   |
|-------|--------|----------|---------------|
| tag   | String | Y        | Tag to attach |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                 |
|---------------|-----------------------|
| Authorization | Bearer <access_token> |

#### Path Parameters

| Parameter | Type   | Required | Description    |
|-----------|--------|----------|----------------|
| item_id   | String | Y        | ID of the item |
| tag       | String | Y        | Tag to remove  |

#### Response Example

```json
{
  "message": "Tag removed successfully"
}
```

---

### GET /categories

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameter | Type   | Required | Description                   |
|-----------|--------|----------|-------------------------------|
| page      | Number | N        | Page number for pagination    |
| limit     | Number | N        | Number of categories per page |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter   | Type   | Required | Description                 |
|-------------|--------|----------|-----------------------------|
| category_id | Number | Y        | ID of the category to fetch |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field       | Type   | Required | Description          |
|-------------|--------|----------|----------------------|
| name        | String | Y        | Name of the category |
| parent_id   | Number | N        | Parent category      |
| description | String | N        | Description          |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter   | Type   | Required | Description                  |
|-------------|--------|----------|------------------------------|
| category_id | Number | Y        | ID of the category to update |

#### Request Body

| Field       | Type   | Required | Description         |
|-------------|--------|----------|---------------------|
| name        | String | Y        | New name            |
| parent_id   | Number | N        | New parent category |
| description | String | N        | New description     |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter   | Type   | Required | Description                  |
|-------------|--------|----------|------------------------------|
| category_id | Number | Y        | ID of the category to delete |

#### Response Example

```json
{
  "message": "Category deleted",
  "deleted": true
}
```

---

### GET /tags

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameter | Type   | Required | Description                   |
|-----------|--------|----------|-------------------------------|
| page      | Number | N        | Page number for pagination    |
| limit     | Number | N        | Number of categories per page |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description   |
|-----------|--------|----------|---------------|
| tag_id    | Number | Y        | ID of the tag |

#### Response Example

```json
{
  "id": 2,
  "name": "Blue"
}
```

---

### POST /tags

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field | Type   | Required | Description     |
|-------|--------|----------|-----------------|
| name  | String | Y        | Name of the tag |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description   |
|-----------|--------|----------|---------------|
| tag_id    | Number | Y        | ID of the tag |

#### Response Example

```json
{
  "message": "Tag successfully deleted"
}
```

---

### GET /orders

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

The query parameters listed below were part of an older draft and are not currently implemented in
the backend controller.

| Parameter | Type   | Required | Description                |
|-----------|--------|----------|----------------------------|
| status    | String | N        | Filter by status           |
| page      | Number | N        | Page number for pagination |
| limit     | Number | N        | Number of orders per page  |

#### Response Example

```json
{
  "orders": [
    {
      "orderId": 5001,
      "userId": 123,
      "itemId": 101,
      "quantity": 1,
      "price": 2.5,
      "feePercentage": 2.5,
      "orderStatus": "pending",
      "createdAt": "2026-02-18T17:00:00",
      "updatedAt": "2026-02-18T17:00:00"
    }
  ]
}
```

---

### GET /orders/{orderId}

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description     |
|-----------|--------|----------|-----------------|
| orderId   | Number | Y        | ID of the order |

#### Response Example

```json
{
  "order": {
    "orderId": 5001,
    "userId": 123,
    "itemId": 101,
    "quantity": 1,
    "price": 2.5,
    "feePercentage": 2.5,
    "orderStatus": "pending",
    "createdAt": "2026-02-18T17:00:00",
    "updatedAt": "2026-02-18T17:00:00"
  }
}
```

---

### POST /orders

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field    | Type   | Required | Description                |
|----------|--------|----------|----------------------------|
| itemId   | Number | Y        | ID of the item to purchase |
| quantity | Number | Y        | Quantity to purchase       |

#### Request Example

```json
{
  "itemId": 100,
  "quantity": 3
}
```

#### Response Example

```json
{
  "order": {
    "orderId": 5003,
    "userId": 1,
    "itemId": 100,
    "quantity": 3,
    "price": 22.33,
    "feePercentage": 2.5,
    "orderStatus": "pending"
  }
}
```

---

### PUT/PATCH /orders/{order_id}

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description     |
|-----------|--------|----------|-----------------|
| order_id  | Number | Y        | ID of the order |

#### Request Body

| Field  | Type   | Required | Description         |
|--------|--------|----------|---------------------|
| status | String | Y        | Update order status |

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

### GET /wishlist

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Response Example

```json
{
  "wishlist": [
    {
      "userId": 1,
      "itemId": 101,
      "createdAt": "2026-02-18T19:00:00"
    }
  ]
}
```

---

### POST /wishlist/{itemId}

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description           |
|-----------|--------|----------|-----------------------|
| itemId    | Number | Y        | ID of the item to add |

#### Response Example

```json
{
  "wishlist": {
    "userId": 1,
    "itemId": 69,
    "createdAt": null
  }
}
```

---

### DELETE /wishlist/{itemId}

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description              |
|-----------|--------|----------|--------------------------|
| itemId    | Number | Y        | ID of the item to remove |

#### Response Example

```json
{
  "message": "Item removed from wishlist",
  "itemId": 101
}
```

---

### GET /wallets

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Response Example

```json
250.75
```

---

### POST /wallets/deposit

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field  | Type   | Required | Description       |
|--------|--------|----------|-------------------|
| amount | Number | Y        | Amount to deposit |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field  | Type   | Required | Description        |
|--------|--------|----------|--------------------|
| amount | Number | Y        | Amount to withdraw |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field             | Type   | Required | Description          |
|-------------------|--------|----------|----------------------|
| amount            | Number | Y        | Amount to withdraw   |
| recipient_user_id | Number | Y        | ID of recipient user |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameter | Type   | Required | Description                     |
|-----------|--------|----------|---------------------------------|
| type      | String | N        | Filter by type                  |
| status    | String | N        | Filter by status                |
| page      | Number | N        | Page number for pagination      |
| limit     | Number | N        | Number of transactions per page |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter      | Type   | Required | Description           |
|----------------|--------|----------|-----------------------|
| transaction_id | Number | Y        | ID of the transaction |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameters | Type   | Required | Description                |
|------------|--------|----------|----------------------------|
| item_id    | Number | N        | Filters reviews by item    |
| user_id    | Number | N        | Filters reviews by user    |
| rating     | Number | N        | Filters review by rating   |
| page       | Number | N        | Page number for pagination |
| limit      | Number | N        | Number of reviews per page |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description      |
|-----------|--------|----------|------------------|
| review_id | Number | Y        | ID of the review |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Request Body

| Field   | Type   | Required | Description                   |
|---------|--------|----------|-------------------------------|
| item_id | Number | Y        | ID of the item being reviewed |
| rating  | Number | Y        | Rating value                  |
| Comment | String | N        | Review text                   |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description      |
|-----------|--------|----------|------------------|
| review_id | Number | Y        | ID of the review |

#### Request Body

| Field   | Type   | Required | Description      |
|---------|--------|----------|------------------|
| rating  | Number | Y        | New rating value |
| Comment | String | N        | New review text  |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description      |
|-----------|--------|----------|------------------|
| review_id | Number | Y        | ID of the review |

#### Response Example

```json
{
  "message": "Review successfully deleted",
  "deleted": true
}
```

---

### GET /notifications

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Query Parameters

| Parameter | Type    | Required | Description                      |
|-----------|---------|----------|----------------------------------|
| unread    | Boolean | N        | Filter by unread                 |
| page      | Number  | N        | Page number for pagination       |
| limit     | Number  | N        | Number of notifications per page |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter       | Type   | Required | Description            |
|-----------------|--------|----------|------------------------|
| notification_id | Number | Y        | ID of the notification |

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

This route is not implemented in the current backend.

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter       | Type   | Required | Description            |
|-----------------|--------|----------|------------------------|
| notification_id | Number | Y        | ID of the notification |

#### Response Example

```json
{
  "id": 8001,
  "read": true,
  "updated_at": "2026-02-18T22:10:00Z"
}
```

---

### GET /cart

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Response Example

```json
{
  "cart": [
    {
      "cartId": 1,
      "userId": 1,
      "itemId": 10,
      "quantity": 2,
      "createdAt": "2026-02-18T19:00:00"
    }
  ]
}
```

---

### POST /cart/{itemId}

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description           |
|-----------|--------|----------|-----------------------|
| itemId    | Number | Y        | ID of the item to add |

#### Query Parameters

| Parameter | Type   | Required | Description                      |
|-----------|--------|----------|----------------------------------|
| quantity  | Number | N        | Quantity to add; defaults to `1` |

#### Response Example

```json
{
  "cart": {
    "cartId": 1,
    "userId": 1,
    "itemId": 10,
    "quantity": 2,
    "createdAt": null
  }
}
```

---

### PUT /cart/{itemId}

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description              |
|-----------|--------|----------|--------------------------|
| itemId    | Number | Y        | ID of the item to update |

#### Query Parameters

| Parameter | Type   | Required | Description  |
|-----------|--------|----------|--------------|
| quantity  | Number | Y        | New quantity |

#### Response Example

```json
{
  "cart": {
    "cartId": 1,
    "userId": 1,
    "itemId": 10,
    "quantity": 3,
    "createdAt": "2026-02-18T19:00:00"
  }
}
```

---

### DELETE /cart/{itemId}

#### Headers

| Header        | Value                   |
|---------------|-------------------------|
| Authorization | Bearer `<access_token>` |

#### Path Parameters

| Parameter | Type   | Required | Description              |
|-----------|--------|----------|--------------------------|
| itemId    | Number | Y        | ID of the item to remove |

#### Response Example

```json
{
  "message": "Item removed from cart",
  "itemId": 10
}
```

---

### GET /api/users

Returns all users as raw `User` entities.

#### Response Example

```json
[
  {
    "userId": 1,
    "email": "user@example.com",
    "status": "ACTIVE",
    "deleted": false
  }
]
```

---

### POST /api/users/addUser

Creates a user from request parameters, not a JSON body.

#### Query Parameters

| Parameter    | Type   | Required | Description                    |
|--------------|--------|----------|--------------------------------|
| email        | String | Y        | User email                     |
| passwordHash | String | Y        | Password hash to save directly |

#### Response Example

```json
{
  "userId": 2,
  "email": "user2@example.com",
  "status": "PENDING",
  "deleted": false
}
```

---

### PUT /api/users/{userId}/profile

Creates or updates a profile using request parameters.

#### Path Parameters

| Parameter | Type   | Required | Description                            |
|-----------|--------|----------|----------------------------------------|
| userId    | Number | Y        | ID of the user whose profile to update |

#### Query Parameters

| Parameter   | Type   | Required | Description      |
|-------------|--------|----------|------------------|
| displayName | String | N        | New display name |
| bio         | String | N        | New bio          |

#### Response Example

```json
{
  "userId": 2,
  "displayName": "LegacyUser",
  "bio": "Legacy profile update"
}
```

---

### GET /api/users/{userId}/profile

Returns the stored profile for the given user ID.

#### Path Parameters

| Parameter | Type   | Required | Description                           |
|-----------|--------|----------|---------------------------------------|
| userId    | Number | Y        | ID of the user whose profile to fetch |

#### Response Example

```json
{
  "userId": 2,
  "displayName": "LegacyUser",
  "bio": "Legacy profile update"
}
```
