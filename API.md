\# Marketplace API Endpoints



This document outlines the main API endpoints for the crypto-powered marketplace.



---



\## 1. Users \& Authentication

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



\## 2. User Profiles

Manages display information for users.



| Endpoint | Method | Description |

|----------|--------|-------------|

| `/profiles/{user\_id}` | GET | Fetch profile info (`display\_name`, `bio`) |

| `/profiles/me` | GET | Fetch profile info for current user |

| `/profiles/me` | PUT/PATCH | Update profile info for current user |



---



\## 3. Items

Marketplace listings management.



| Endpoint | Method | Description |

|----------|--------|-------------|

| `/items` | GET | List all items (filter by category, tag, price) |

| `/items/{item\_id}` | GET | Fetch single item details |

| `/items` | POST | Create a new item listing |

| `/items/{item\_id}` | PUT/PATCH | Update item info (price, stock, description) |

| `/items/{item\_id}` | DELETE | Soft delete an item |

| `/items/{item\_id}/images` | POST | Upload images for item |

| `/items/{item\_id}/images/{image\_id}` | DELETE | Remove item image |

| `/items/{item\_id}/tags` | POST | Attach a tag to an item |

| `/items/{item\_id}/tags/{tag\_id}` | DELETE | Remove tag from item |



---



\## 4. Categories



| Endpoint | Method | Description |

|----------|--------|-------------|

| `/categories` | GET | List all categories |

| `/categories/{category\_id}` | GET | Get category details (including parent/children) |

| `/categories` | POST | Create a new category |

| `/categories/{category\_id}` | PUT/PATCH | Update category info |

| `/categories/{category\_id}` | DELETE | Soft delete category |



---



\## 5. Tags



| Endpoint | Method | Description |

|----------|--------|-------------|

| `/tags` | GET | List all tags |

| `/tags/{tag\_id}` | GET | Get tag info |

| `/tags` | POST | Create a new tag |

| `/tags/{tag\_id}` | DELETE | Delete tag |



---



\## 6. Orders

Buying and selling workflow.



| Endpoint | Method | Description |

|----------|--------|-------------|

| `/orders` | GET | List user orders |

| `/orders/{order\_id}` | GET | Get order details |

| `/orders` | POST | Place a new order |

| `/orders/{order\_id}` | PUT/PATCH | Update order info (cancel, etc.) |

| `/orders/{order\_id}/status` | PATCH | Update order status (`pending`, `completed`, `cancelled`) |



---



\## 7. Wishlist



| Endpoint | Method | Description |

|----------|--------|-------------|

| `/wishlist` | GET | List items in user’s wishlist |

| `/wishlist` | POST | Add item to wishlist |

| `/wishlist/{item\_id}` | DELETE | Remove item from wishlist |



---



\## 8. Crypto Transactions \& Wallets



| Endpoint | Method | Description |

|----------|--------|-------------|

| `/wallets` | GET | Fetch wallet balance |

| `/wallets/deposit` | POST | Deposit crypto to wallet |

| `/wallets/withdraw` | POST | Withdraw crypto from wallet |

| `/wallets/transfer` | POST | Transfer crypto from wallet to a different wallet |

| `/transactions` | GET | List user transactions |

| `/transactions/{transaction\_id}` | GET | Get transaction details |



---



\## 9. Reviews



| Endpoint | Method | Description |

|----------|--------|-------------|

| `/reviews` | GET | List all reviews (filter by item/user) |

| `/reviews/{review\_id}` | GET | Fetch single review |

| `/reviews` | POST | Create a new review for an item |

| `/reviews/{review\_id}` | PUT/PATCH | Update review |

| `/reviews/{review\_id}` | DELETE | Soft delete review |



---



\## 10. Notifications



| Endpoint | Method | Description |

|----------|--------|-------------|

| `/notifications` | GET | List notifications for user |

| `/notifications/{notification\_id}` | GET | Fetch notification details |

| `/notifications/{notification\_id}/read` | PATCH | Mark notification as read |



---

