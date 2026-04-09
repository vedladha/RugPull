import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../Auth/auth-context.js";
import "../style/order-page.css";

const API = "http://localhost:3001";
const MARKETPLACE_FEE_RATE = 0.025;

function parsePrice(value) {
  const parsed = Number.parseFloat(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatCurrency(value) {
  return `$${value.toFixed(2)}`;
}

function normalizeItems(state) {
  const sourceItems = Array.isArray(state?.items)
    ? state.items
    : state?.item
      ? [state.item]
      : [];

  return sourceItems
    .filter((item) => item?.itemId != null)
    .map((item, index) => {
      const quantity = Number.isFinite(Number(item.quantity)) && Number(item.quantity) > 0
        ? Number(item.quantity)
        : 1;
      const stock = Number.isFinite(Number(item.stock)) ? Number(item.stock) : null;
      const safeQuantity = stock !== null && stock > 0 ? Math.min(quantity, stock) : quantity;

      return {
        itemId: item.itemId,
        name: item.name || `Listing ${index + 1}`,
        description: item.description || "No description provided.",
        price: parsePrice(item.price),
        sellerName: item.sellerName || "Marketplace Seller",
        quantity: safeQuantity,
        stock,
        fromCart: Boolean(item.fromCart),
      };
    });
}

export default function OrderPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, walletBalance } = useAuth();

  const [orderItems, setOrderItems] = useState(() => normalizeItems(location.state));
  const [wallet, setWallet] = useState(null);
  const [walletError, setWalletError] = useState("");
  const [submissionError, setSubmissionError] = useState("");
  const [successfulOrder, setSuccessfulOrder] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setOrderItems(normalizeItems(location.state));
    setSubmissionError("");
    setSuccessfulOrder(null);
  }, [location.state]);

  useEffect(() => {
    if (!user) {
      setWallet(null);
      return;
    }

    walletBalance()
      .then((balance) => {
        setWallet(balance);
        setWalletError("");
      })
      .catch((error) => {
        setWallet(null);
        setWalletError(error.message || "Unable to load wallet balance");
      });
  }, [user, walletBalance]);

  const subtotal = orderItems.reduce((total, item) => total + (item.price * item.quantity), 0);
  const fee = subtotal * MARKETPLACE_FEE_RATE;
  const total = subtotal + fee;
  const totalQuantity = orderItems.reduce((count, item) => count + item.quantity, 0);
  const sourceLabel = location.state?.source === "cart" ? "cart checkout" : "buy it now";
  const hasUnavailableItems = orderItems.some((item) => item.stock !== null && item.stock <= 0);

  const updateQuantity = (itemId, nextQuantity) => {
    setOrderItems((currentItems) =>
      currentItems.map((item) => {
        if (item.itemId !== itemId) return item;

        const boundedQuantity = item.stock !== null && item.stock > 0
          ? Math.min(Math.max(nextQuantity, 1), item.stock)
          : Math.max(nextQuantity, 1);

        return {
          ...item,
          quantity: boundedQuantity,
        };
      }),
    );
  };

  const reloadWallet = async () => {
    if (!user) return;

    try {
      const balance = await walletBalance();
      setWallet(balance);
      setWalletError("");
    } catch (error) {
      setWallet(null);
      setWalletError(error.message || "Unable to load wallet balance");
    }
  };

  const handlePlaceOrder = async () => {
    if (orderItems.length === 0 || submitting) return;

    setSubmitting(true);
    setSubmissionError("");
    try {
      const response = await fetch(`${API}/orders`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
          items: orderItems.map((item) => ({
            itemId: item.itemId,
            quantity: item.quantity,
          })),
        }),
      });

      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.error || "Failed to place order");
      }

      const cartItems = orderItems.filter((item) => item.fromCart);
      await Promise.allSettled(
        cartItems.map((item) =>
          fetch(`${API}/cart/${item.itemId}`, {
            method: "DELETE",
            credentials: "include",
          }),
        ),
      );

      setSuccessfulOrder({
        orderId: payload.order?.orderId ?? null,
        itemCount: orderItems.length,
      });
      setOrderItems([]);
      await reloadWallet();
    } catch (error) {
      setSubmissionError(error.message || "Failed to place order");
    } finally {
      setSubmitting(false);
    }
  };

  if (!user) {
    return (
      <section className="order-page">
        <div className="order-shell order-empty-state">
          <p className="order-eyebrow">Order review</p>
          <h1>Sign in to complete your order.</h1>
          <p className="order-empty-copy">
            This checkout flow requires an authenticated account.
          </p>
          <button
            type="button"
            className="btn-primary"
            onClick={() => navigate("/login")}
          >
            Go to Sign In
          </button>
        </div>
      </section>
    );
  }

  if (orderItems.length === 0 && !successfulOrder) {
    return (
      <section className="order-page">
        <div className="order-shell order-empty-state">
          <p className="order-eyebrow">Order review</p>
          <h1>No items are ready for checkout.</h1>
          <p className="order-empty-copy">
            Start from a listing or your cart to build an order.
          </p>
          <button
            type="button"
            className="btn-primary"
            onClick={() => navigate("/listings")}
          >
            Browse Marketplace
          </button>
        </div>
      </section>
    );
  }

  if (orderItems.length === 0 && successfulOrder) {
    return (
      <section className="order-page">
        <div className="order-shell order-empty-state">
          <div className="order-feedback order-feedback-success" role="status">
            <h2>Order submitted.</h2>
            <p>
              Order {successfulOrder.orderId ? `#${successfulOrder.orderId} ` : ""}created successfully
              with {successfulOrder.itemCount} {successfulOrder.itemCount === 1 ? "item" : "items"}.
            </p>
          </div>
          <button
            type="button"
            className="btn-primary"
            onClick={() => navigate("/listings")}
          >
            Browse Marketplace
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="order-page">
      <div className="order-shell">
        <header className="order-header">
          <div>
            <p className="order-eyebrow">Order review</p>
            <h1>Confirm your order.</h1>
          </div>
          <p className="order-subtitle">
            Review the items from your {sourceLabel}, adjust quantities, and place the order.
          </p>
        </header>

        {successfulOrder && (
          <div className="order-feedback order-feedback-success" role="status">
            <h2>Order submitted.</h2>
            <p>
              Order {successfulOrder.orderId ? `#${successfulOrder.orderId} ` : ""}created successfully
              with {successfulOrder.itemCount} {successfulOrder.itemCount === 1 ? "item" : "items"}.
            </p>
          </div>
        )}

        {submissionError && (
          <div className="order-feedback order-feedback-error" role="alert">
            {submissionError}
          </div>
        )}

        <div className="order-layout">
          <div className="order-items-panel">
            {orderItems.map((item) => {
              const lineTotal = item.price * item.quantity;
              const atStockLimit = item.stock !== null && item.quantity >= item.stock;

              return (
                <article className="order-item-card" key={item.itemId}>
                  <div className="order-item-copy">
                    <div className="order-item-meta">
                      <span className="order-item-origin">
                        {item.fromCart ? "From cart" : "Buy now"}
                      </span>
                      <span className="order-item-seller">Seller: {item.sellerName}</span>
                    </div>
                    <h2>{item.name}</h2>
                    <p>{item.description}</p>
                  </div>

                  <div className="order-item-controls">
                    <div className="order-quantity">
                      <span>Quantity</span>
                      <div className="quantity-controls">
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.itemId, item.quantity - 1)}
                          disabled={item.quantity <= 1}
                          aria-label={`Decrease quantity for ${item.name}`}
                        >
                          -
                        </button>
                        <span>{item.quantity}</span>
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.itemId, item.quantity + 1)}
                          disabled={atStockLimit}
                          aria-label={`Increase quantity for ${item.name}`}
                        >
                          +
                        </button>
                      </div>
                    </div>

                    <div className="order-line-total">
                      <span>Line total</span>
                      <strong>{formatCurrency(lineTotal)}</strong>
                    </div>
                  </div>

                  <div className="order-item-footer">
                    <span>{formatCurrency(item.price)} each</span>
                    <span>
                      {item.stock !== null
                        ? item.stock > 0
                          ? `${item.stock} in stock`
                          : "Sold out"
                        : "Stock information unavailable"}
                    </span>
                  </div>
                </article>
              );
            })}
          </div>

          <aside className="order-summary">
            <h2>Summary</h2>
            <div className="order-summary-stack">
              <div className="summary-row">
                <span>Items</span>
                <span>{totalQuantity}</span>
              </div>
              <div className="summary-row">
                <span>Subtotal</span>
                <span>{formatCurrency(subtotal)}</span>
              </div>
              <div className="summary-row">
                <span>Marketplace fee</span>
                <span>{formatCurrency(fee)}</span>
              </div>
              <div className="modal-divider"></div>
              <div className="summary-row total-row">
                <span>Total</span>
                <span>{formatCurrency(total)}</span>
              </div>
            </div>

            <div className="order-balance-card">
              <span className="order-balance-label">Wallet balance</span>
              <strong>
                {walletError
                  ? "Unavailable"
                  : wallet !== null
                    ? `${wallet.toFixed(2)} RPC`
                    : "Loading..."}
              </strong>
              <p>
                {wallet !== null
                  ? `After this order: ${(wallet - total).toFixed(2)} RPC`
                  : "The wallet total refreshes again after successful checkout."}
              </p>
            </div>

            {hasUnavailableItems && (
              <div className="order-feedback order-feedback-error" role="alert">
                Remove sold out items before placing this order.
              </div>
            )}

            <button
              type="button"
              className="btn-primary order-submit-btn"
              onClick={handlePlaceOrder}
              disabled={submitting || orderItems.length === 0 || hasUnavailableItems}
            >
              {submitting
                ? "Placing order..."
                : `Place ${orderItems.length > 1 ? "orders" : "order"}`}
            </button>

            <button
              type="button"
              className="btn-ghost"
              onClick={() => navigate(location.state?.source === "cart" ? "/cart" : "/listings")}
            >
              {location.state?.source === "cart" ? "Back to Cart" : "Back to Listings"}
            </button>
          </aside>
        </div>
      </div>
    </section>
  );
}
