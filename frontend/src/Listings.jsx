import { useState, useEffect } from "react";
import ListingCard from "./Components/ListingCard.jsx";

export default function Listings() {
  const [listings, setListings] = useState([]);
  const [filteredListings, setFilteredListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [priceFilter, setPriceFilter] = useState({ min: "", max: "" });
  const [keywordFilter, setKeywordFilter] = useState("");

  const API = "http://localhost:3001";

  useEffect(() => {
    fetchListings();
  }, []);

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
          listing.title.toLowerCase().includes(keywordFilter.toLowerCase()) ||
          listing.bio.toLowerCase().includes(keywordFilter.toLowerCase())
        );

      setFilteredListings(filtered);
    };

    filterListings();
  });

  const fetchListings = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API}/items`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      setListings(data.items || []);
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
              key={listing.id}
              name={listing.name}
              description={listing.description}
              price={listing.price}
              seller={listing.sellerName}
            />
          ))
        )}
      </div>
    </div>
  );
}
