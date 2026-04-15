export default function ListingCard({
  name,
  description,
  price,
  stock,
  seller,
  onClick,
}) {
  const quantity = Number.isFinite(Number(stock)) ? Number(stock) : null;
  const isSoldOut = quantity !== null && quantity <= 0;
    const getImageUrl = (url) => {
      if (!url) return null;
      // If it's a local preview (blob:), return it directly.
      // Otherwise, append the backend API prefix.
      return url.startsWith("blob:") ? url : `http://localhost:3001${url}`;
    };

  return (
    <button
      type="button"
      className={`listing-card listing-card-button ${isSoldOut ? "listing-card-sold-out" : ""}`}
      onClick={onClick}
      aria-label={`View details for ${name}`}
    >
          <div className="card-image-container">
            {thumbnail_url ? (
              <img
                src={getImageUrl(thumbnail_url)}
                alt={name}
                className="card-image"
              />
            ) : (
              <div className="placeholder-image">No Image Available</div>
            )}
          </div>
      <div className="listing-card-header">
        <div className="listing-title">{name}</div>
        <div className={`listing-status ${isSoldOut ? "listing-status-sold-out" : ""}`}>
          {isSoldOut
            ? "Sold Out"
            : quantity !== null
              ? `${quantity} left`
              : "Stock unavailable"}
        </div>
      </div>
      <div className="listing-bio">{description}</div>
      <div className="listing-price">{price}</div>
      <div className="listing-stock">
        {quantity !== null
          ? `Quantity available: ${Math.max(quantity, 0)}`
          : "Quantity available: Unknown"}
      </div>
      <div className="listing-seller">Seller: {seller}</div>
    </button>
  );
}
