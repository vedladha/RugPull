# Frontend

This frontend is a React 19 + Vite 7 application for the $RPC marketplace.

## Main Routes

- `/` landing page
- `/login` and `/signup` authentication page
- `/listings` marketplace listings
- `/sell` item creation flow
- `/profile` profile management
- `/wishlist` wishlist page
- `/cart` shopping cart
- `/order` order checkout flow
- `/history` order history
- `/earn` daily reward, ads, wallet funding, slot machine, and roulette

## Auth And API Usage

- Authentication uses the backend's HTTP-only `jwt` cookie flow
- Shared auth, profile, wallet, wishlist, and ratings helpers live in `src/Auth/AuthContext.jsx`
- The frontend talks to the backend at `http://localhost:3001`

## Scripts

```bash
npm install
npm run dev
npm run build
npm run lint
npm test
```
