export default function ListingCard({ name, description, price, seller, onClick }) {
  return (
    <button
      type="button"
      className="listing-card listing-card-button"
      onClick={onClick}
      aria-label={`View details for ${name}`}
    >
      <div className="listing-title">{name}</div>
      <div className="listing-bio">{description}</div>
      <div className="listing-price">{price}</div>
      <div className="listing-seller">Seller: {seller}</div>
    </button>
  );
}
