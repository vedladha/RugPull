import { useState, useEffect } from "react";
import ListingCard from "./Components/ListingCard.jsx";
import ListingModal from "./Components/ListingModal.jsx";
import { useAuth } from "./Auth/auth-context";

export default function Listings() {
  const { user, getWishlist, addToWishlist, removeFromWishlist } = useAuth();
  const [listings, setListings] = useState([]);
  const [filteredListings, setFilteredListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [priceFilter, setPriceFilter] = useState({ min: "", max: "" });
  const [keywordFilter, setKeywordFilter] = useState("");
  const [selectedListing, setSelectedListing] = useState(null);
  const [wishlistItemIds, setWishlistItemIds] = useState(new Set());
  const [wishlistError, setWishlistError] = useState("");
  const [wishlistSuccess, setWishlistSuccess] = useState("");
  const [wishlistBusyItemId, setWishlistBusyItemId] = useState(null);

  const API = "http://localhost:3001";

  useEffect(() => {
    fetchListings();
  }, []);

  useEffect(() => {
    if (!user) {
      setWishlistItemIds(new Set());
      setWishlistError("");
      setWishlistSuccess("");
      return;
    }

    getWishlist()
      .then((wishlist) => {
        setWishlistItemIds(new Set(wishlist.map((entry) => entry.itemId)));
      })
      .catch((err) => {
        setWishlistError(err.message);
      });
  }, [user, getWishlist]);

  useEffect(() => {
    if (!wishlistSuccess) {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setWishlistSuccess("");
    }, 3000);

    return () => window.clearTimeout(timeoutId);
  }, [wishlistSuccess]);

  useEffect(() => {
    const filterListings = () => {
      let filtered = listings;

      if (priceFilter.min !== "") {
        const minPrice = parseFloat(priceFilter.min);
        filtered = filtered.filter((listing) => {
          const price = parseFloat(String(listing.price).replace(/[^\d.]/g, ""));
          return price >= minPrice;
        });
      }

      if (priceFilter.max !== "") {
        const maxPrice = parseFloat(priceFilter.max);
        filtered = filtered.filter((listing) => {
          const price = parseFloat(String(listing.price).replace(/[^\d.]/g, ""));
          return price <= maxPrice;
        });
      }

      if (keywordFilter !== "")
        filtered = filtered.filter((listing) =>
          listing.name.toLowerCase().includes(keywordFilter.toLowerCase()) ||
          listing.description.toLowerCase().includes(keywordFilter.toLowerCase())
        );

      setFilteredListings(filtered);
    };

    filterListings();
  }, [listings, priceFilter, keywordFilter]);

  const fetchListings = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API}/items`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      const normalizedListings = (data.items || []).map((item) => ({
        ...item,
        id: item.itemId,
      }));
      setListings(normalizedListings);
    } catch (err) {
      setError(err.message);
      console.error("Error fetching listings:", err);
    } finally {
      setLoading(false);
    }
  };

  const handlePriceFilterChange = (type, value) => {
    setPriceFilter((prev) => ({
      ...prev,
      [type]: value === "" ? "" : Math.max(0, parseFloat(value)),
    }));
  };

  const handlePriceFilterBlur = (type) => {
    setPriceFilter((prev) => {
      const updated = { ...prev };
      if (updated.min !== "" && updated.max !== "") {
        if (type === "min" && updated.min > updated.max)
          updated.max = updated.min;
        if (type === "max" && updated.max < updated.min)
          updated.min = updated.max;
      }
      return updated;
    });
  };

  const handleToggleWishlist = async (itemId) => {
    setWishlistError("");
    setWishlistSuccess("");

    if (!user) {
      setWishlistError("Sign in to save items to your wishlist.");
      return;
    }

    try {
      setWishlistBusyItemId(itemId);

      if (wishlistItemIds.has(itemId)) {
        await removeFromWishlist(itemId);
        setWishlistItemIds((prev) => {
          const updated = new Set(prev);
          updated.delete(itemId);
          return updated;
        });
        setWishlistSuccess("Item removed from wishlist");
      } else {
        await addToWishlist(itemId);
        setWishlistItemIds((prev) => new Set(prev).add(itemId));
        setWishlistSuccess("Item added to wishlist");
      }
    } catch (err) {
      setWishlistError(err.message);
    } finally {
      setWishlistBusyItemId(null);
    }
  };

  const handleOpenListing = (listing) => {
    setWishlistError("");
    setWishlistSuccess("");
    setSelectedListing(listing);
  };

  const handleCloseListing = () => {
    setWishlistError("");
    setWishlistSuccess("");
    setWishlistBusyItemId(null);
    setSelectedListing(null);
  };

  if (loading) {
    return (
      <div className="listings-section">
        <h2>Active Listings</h2>
        <div className="loading">Loading listings...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="listings-section">
        <h2>Active Listings</h2>
        <div className="error">Error loading listings: Please Try Again</div>
      </div>
    );
  }

  return (
    <div className="listings-section">
      <h2>Active Listings</h2>

      <div className="filters-section">
        <div className="filter-group">
          <label className="filter-label">Min Price:</label>
          <input
            type="number"
            className="filter-input"
            value={priceFilter.min}
            onChange={(e) => handlePriceFilterChange("min", e.target.value)}
            onBlur={() => handlePriceFilterBlur("min")}
            placeholder="0"
            step="0.01"
          />
        </div>
        <div className="filter-group">
          <label className="filter-label">Max Price:</label>
          <input
            type="number"
            className="filter-input"
            value={priceFilter.max}
            onChange={(e) => handlePriceFilterChange("max", e.target.value)}
            onBlur={() => handlePriceFilterBlur("max")}
            placeholder="No limit"
            step="0.01"
          />
        </div>

        <div className="filter-group">
          <label className="filter-label">Keyword:</label>
          <input
            type="text"
            className="filter-input"
            value={keywordFilter}
            onChange={(e) => setKeywordFilter(e.target.value)}
            placeholder="Search listings..."
          />
        </div>
      </div>

      <div className="listings-grid">
        {filteredListings.length === 0 ? (
          <div className="no-listings">
            No listings found matching your filters.
          </div>
        ) : (
          filteredListings.map((listing) => (
            <ListingCard
              key={listing.itemId}
              name={listing.name}
              description={listing.description}
              price={listing.price}
              stock={listing.stock}
              seller={listing.sellerName}
              thumbnail_url={listing.thumbnailUrl}
              onClick={() => handleOpenListing(listing)}
            />
          ))
        )}
      </div>

      {selectedListing && (
        <ListingModal
          listing={selectedListing}
          onClose={handleCloseListing}
          isWishlisted={wishlistItemIds.has(selectedListing.id)}
          onToggleWishlist={() => handleToggleWishlist(selectedListing.id)}
          wishlistBusy={wishlistBusyItemId === selectedListing.id}
          wishlistError={wishlistError}
          wishlistSuccess={wishlistSuccess}
        />
      )}
    </div>
  );
}
