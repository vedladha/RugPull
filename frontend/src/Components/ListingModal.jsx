import { useEffect, useState } from "react";
import "../style/listing-modal.css"

const API = "http://localhost:3001";

export default function ListingModal({ listing, onClose }) {
  const [addingToCart, setAddingToCart] = useState(false)

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

  const addToCart = async () => {
    setAddingToCart(true);
    
    await fetch(`${API}/cart/${listing.itemId}`, {
      method: "POST",
      credentials: "include"
    }).then(() => setAddingToCart(false));
  }

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

        <div className="listing-modal-actions">
          <button type="button" className="listing-action-btn listing-action-btn-primary">
            Buy
          </button>
          <button type="button" className="listing-action-btn listing-action-btn-secondary" onClick={addToCart}>
            {addingToCart ? "Adding to your Cart" : "Add to Cart"}
          </button>
        </div>
      </div>
    </div >
  );
}
