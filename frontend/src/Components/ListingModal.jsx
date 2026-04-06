import { useEffect } from "react";
import "../style/listing-modal.css"

export default function ListingModal({
  listing,
  onClose,
  isWishlisted = false,
  onToggleWishlist,
  wishlistBusy = false,
  wishlistError = "",
  wishlistSuccess = "",
}) {
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

  if (!listing) return null;

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
          X
        </button>

        <h3 id="listing-modal-title" className="listing-modal-title">
          {listing.name}
        </h3>

        <p className="listing-modal-description">
          {listing.description || "No description provided."}
        </p>

        <div className="listing-modal-meta">
          <div className="listing-modal-price">{listing.price}</div>
          <div className="listing-modal-seller">Seller: {listing.sellerName}</div>
        </div>

        {wishlistError && (
          <div className="listing-modal-feedback listing-modal-feedback-error" role="alert">
            {wishlistError}
          </div>
        )}

        {wishlistSuccess && (
          <div className="listing-modal-feedback listing-modal-feedback-success" role="status">
            {wishlistSuccess}
          </div>
        )}

        <div className="listing-modal-actions">
          <button type="button" className="listing-action-btn listing-action-btn-primary">
            Buy
          </button>
          <button type="button" className="listing-action-btn listing-action-btn-secondary">
            Add to Cart
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
