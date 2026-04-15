import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../style/listing-modal.css";

const API = "http://localhost:3001";
export default function ListingModal({
  listing,
  onClose,
  isWishlisted = false,
  onToggleWishlist,
  wishlistBusy = false,
  wishlistError = "",
  wishlistSuccess = "",
}) {
  const navigate = useNavigate();
  const [addingToCart, setAddingToCart] = useState(false);
  const [cartFeedback, setCartFeedback] = useState("");
  const [cartError, setCartError] = useState("");
  const stock = Number.isFinite(Number(item?.stock)) ? Number(item.stock) : null;
  const isSoldOut = stock !== null && stock <= 0;
  const [quantity, setQuantity] = useState(
    stock !== null && stock > 0 ? "1" : "0",
  );
  useEffect(() => {
    const handleEscape = (event) => {
      if (event.key === "Escape") onClose();
    };

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    window.addEventListener("keydown", handleEscape);
    return () => {
      window.removeEventListener("keydown", handleEscape);
      document.body.style.overflow = previousOverflow;
    };
  }, [onClose]);

  useEffect(() => {
    setCartFeedback("");
    setCartError("");
    setAddingToCart(false);
    setQuantity(stock !== null && stock > 0 ? "1" : "0");
  }, [listing?.itemId, stock]);

  if (!listing) return null;
  const { item, images } = listing;

  const maxQuantity = stock !== null && stock > 0 ? stock : null;
  const parsedQuantity = Number(quantity);
  const selectedQuantity = Number.isInteger(parsedQuantity) && parsedQuantity > 0
    ? maxQuantity !== null
      ? Math.min(parsedQuantity, maxQuantity)
      : parsedQuantity
    : 1;

  const handleQuantityChange = (value) => {
    if (value === "") {
      setQuantity("");
      return;
    }

    const nextValue = Number(value);
    if (!Number.isFinite(nextValue)) {
      return;
    }

    const boundedValue = maxQuantity !== null
      ? Math.min(Math.max(Math.trunc(nextValue), 1), maxQuantity)
      : Math.max(Math.trunc(nextValue), 1);

    setQuantity(String(boundedValue));
  };

  const handleQuantityBlur = () => {
    handleQuantityChange(quantity === "" ? "1" : quantity);
  };

  const addToCart = async () => {
    if (isSoldOut) {
      return;
    }

    setAddingToCart(true);
    setCartFeedback("");
    setCartError("");

    try {
      const cartResponse = await fetch(`${API}/cart`, {
        method: "GET",
        credentials: "include",
      });

      const cartPayload = await cartResponse.json().catch(() => ({}));
      if (!cartResponse.ok) {
        throw new Error(cartPayload.error || "Failed to add item to cart");
      }

      const cartItems = Array.isArray(cartPayload.cart)
        ? cartPayload.cart
        : Array.isArray(cartPayload)
          ? cartPayload
          : [];
      const existingCartItem = cartItems.find((item) => item.itemId === item.itemId);
      const nextQuantity = existingCartItem
        ? existingCartItem.quantity + selectedQuantity
        : selectedQuantity;

      if (stock !== null && nextQuantity > stock) {
        throw new Error("Cart is over the limit for this item");
      }

      const response = await fetch(
        `${API}/cart/${item.itemId}?quantity=${nextQuantity}`,
        {
          method: existingCartItem ? "PUT" : "POST",
          credentials: "include",
        },
      );

      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.error || "Failed to add item to cart");
      }

      setCartFeedback(
        existingCartItem
          ? `Cart updated to ${nextQuantity} ${nextQuantity === 1 ? "item" : "items"} for this listing.`
          : `${selectedQuantity} ${selectedQuantity === 1 ? "item" : "items"} added to your cart.`,
      );
    } catch (error) {
      setCartError(error.message || "Failed to add item to cart");
    } finally {
      setAddingToCart(false);
    }
  };

  return (
    <div className="listing-modal-overlay" onClick={onClose} role="presentation">
      <div
        className="listing-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="listing-modal-title"
        onClick={(event) => event.stopPropagation()}
      >
        <button
          type="button"
          className="listing-modal-close"
          onClick={onClose}
          aria-label="Close listing details"
        >
          &times;
        </button>

        <div className="listing-modal-gallery">
          {images && images.length > 0 ? (
            images.map((img) => (
              <img
                key={img.imageId}
                src={img.imageUrl}
                alt={item.name}
                className="modal-image"
              />
            ))
          ) : (
            <div className="placeholder-image">No images available</div>
          )}
        </div>

        <div className="listing-modal-content">
          <h3 id="listing-modal-title" className="listing-modal-title">
            {item.name}
          </h3>

          <p className="listing-modal-description">
            {item.description || "No description provided."}
          </p>

        <div className="listing-modal-meta">
          <div className="listing-modal-price">{item.price}</div>
          <div className="listing-modal-seller">Seller: {item.sellerName}</div>
        </div>

        <div className="listing-modal-stock-row">
          <div className={`listing-modal-stock ${isSoldOut ? "listing-modal-stock-sold-out" : ""}`}>
            {stock !== null
              ? isSoldOut
                ? "Sold Out"
                : `${stock} available`
              : "Stock unavailable"}
          </div>
          <label className="listing-modal-quantity">
            <span>Quantity</span>
            <input
              type="number"
              min="1"
              max={maxQuantity ?? undefined}
              step="1"
              value={quantity}
              onChange={(event) => handleQuantityChange(event.target.value)}
              onBlur={handleQuantityBlur}
              disabled={isSoldOut}
              aria-label="Quantity"
            />
          </label>
        </div>

        {wishlistError && (
          <div className="listing-modal-feedback listing-modal-feedback-error" role="alert">
            {wishlistError}
          </div>
        )}

        {cartError && (
          <div className="listing-modal-feedback listing-modal-feedback-error" role="alert">
            {cartError}
          </div>
        )}

        {cartFeedback && (
          <div className="listing-modal-feedback listing-modal-feedback-success" role="status">
            {cartFeedback}
          </div>
        )}

        {wishlistSuccess && (
          <div className="listing-modal-feedback listing-modal-feedback-success" role="status">
            {wishlistSuccess}
          </div>
        )}

        <div className="listing-modal-actions">
          <button
            type="button"
            className="listing-action-btn listing-action-btn-primary"
            onClick={() => navigate("/order", {
              state: {
                source: "listing",
                items: [
                  {
                    ...listing,
                    quantity: selectedQuantity,
                    fromCart: false,
                  },
                ],
              },
            })}
            disabled={isSoldOut}
          >
            {isSoldOut ? "Sold Out" : "Buy It Now"}
          </button>
          <button
            type="button"
            className="listing-action-btn listing-action-btn-secondary"
            onClick={addToCart}
            disabled={addingToCart || isSoldOut}
          >
            {isSoldOut
              ? "Sold Out"
              : addingToCart
                ? "Adding to your Cart"
                : "Add to Cart"}
          </button>
          <button
            type="button"
            className="listing-action-btn listing-action-btn-secondary"
            onClick={onToggleWishlist}
            disabled={wishlistBusy}
          >
            {wishlistBusy
              ? "Saving..."
              : isWishlisted
                ? "Remove from Wishlist"
                : "Add to Wishlist"}
          </button>
        </div>
      </div>
    </div>
  );
}
