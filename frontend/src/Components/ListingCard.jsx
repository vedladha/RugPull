export default function ListingCard({ title, bio, price, seller }) {
  return (
    <div className="listing-card">
      <div className="listing-title">{title}</div>
      <div className="listing-bio">{bio}</div>
      <div className="listing-price">{price}</div>
      <div className="listing-seller">Seller: {seller}</div>
    </div>
  );
}
