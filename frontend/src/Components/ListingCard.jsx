export default function ListingCard({ name, description, price, seller }) {
  return (
    <div className="listing-card">
      <div className="listing-title">{name}</div>
      <div className="listing-bio">{description}</div>
      <div className="listing-price">{price}</div>
      <div className="listing-seller">Seller: {seller}</div>
    </div>
  );
}
