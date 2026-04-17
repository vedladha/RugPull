import { useEffect, useState } from "react";
import { useAuth } from "./Auth/auth-context";
import ListingModal from "./Components/ListingModal.jsx";

export default function WishlistPage() {
  const { user, getWishlistItems, removeFromWishlist } = useAuth();
  const [wishlistItems, setWishlistItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [busyItemId, setBusyItemId] = useState(null);
  const [selectedListing, setSelectedListing] = useState(null);

  useEffect(() => {
    if (!user) {
      setWishlistItems([]);
      setLoading(false);
      setError("");
      setSuccess("");
      return;
    }

    setLoading(true);
    setError("");

    getWishlistItems()
      .then((items) => {
        setWishlistItems(items);
      })
      .catch((err) => {
        setError(err.message);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [user, getWishlistItems]);

  useEffect(() => {
    if (!success) {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setSuccess("");
    }, 3000);

    return () => window.clearTimeout(timeoutId);
  }, [success]);

  const handleRemove = async (itemId) => {
    setError("");
    setSuccess("");
    setBusyItemId(itemId);

    try {
      await removeFromWishlist(itemId);
      setWishlistItems((prev) => prev.filter((item) => item.itemId !== itemId));
      setSelectedListing((prev) => (prev?.itemId === itemId ? null : prev));
      setSuccess("Item removed from wishlist");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyItemId(null);
    }
  };

  const handleOpenListing = (item) => {
    setError("");
    setSuccess("");
    setSelectedListing(item);
  };

  const handleCloseListing = () => {
    setError("");
    setSuccess("");
    setBusyItemId(null);
    setSelectedListing(null);
  };

  if (!user) {
    return (
      <div className="listings-section">
        <h2>Your Wishlist</h2>
        <div className="error">Sign in to view your wishlist.</div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="listings-section">
        <h2>Your Wishlist</h2>
        <div className="loading">Loading wishlist...</div>
      </div>
    );
  }

  if (error && !wishlistItems.length) {
    return (
      <div className="listings-section">
        <h2>Your Wishlist</h2>
        <div className="error">{error}</div>
      </div>
    );
  }

  return (
    <div className="listings-section">
      <h2>Your Wishlist</h2>

      {success && (
        <div className="wishlist-feedback wishlist-feedback-success" role="status">
          {success}
        </div>
      )}

      {wishlistItems.length === 0 ? (
        <div className="no-listings">Your wishlist is empty.</div>
      ) : (
        <div className="listings-grid">
          {wishlistItems.map((item) => (
            <div className="listing-card wishlist-card" key={item.itemId}>
              <button
                type="button"
                className="listing-card-button wishlist-card-open"
                onClick={() => handleOpenListing(item)}
                aria-label={`View details for ${item.name}`}
              >
                <div className="listing-title">{item.name}</div>
                <div className="listing-bio">{item.description}</div>
                <div className="listing-price">{item.price}</div>
                <div className="listing-seller">Seller: {item.sellerName}</div>
              </button>
              <div className="wishlist-card-actions">
                <button
                  type="button"
                  className="wishlist-action-btn"
                  onClick={() => handleRemove(item.itemId)}
                  disabled={busyItemId === item.itemId}
                >
                  {busyItemId === item.itemId ? "Removing..." : "Remove from Wishlist"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {error && wishlistItems.length > 0 ? (
        <div className="error">{error}</div>
      ) : null}

      {selectedListing && (
        <ListingModal
          listing={selectedListing}
          onClose={handleCloseListing}
          isWishlisted
          onToggleWishlist={() => handleRemove(selectedListing.itemId)}
          wishlistBusy={busyItemId === selectedListing.itemId}
          wishlistError={error}
          wishlistSuccess={success}
        />
      )}
    </div>
  );
}