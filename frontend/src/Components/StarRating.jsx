export default function StarRating({ value = 0, size = "medium" }) {
  const clamped = Math.max(0, Math.min(5, Number(value) || 0));
  const fillPercent = (clamped / 5) * 100;

  return (
    <span
      className={`star-rating star-rating-${size}`}
      role="img"
      aria-label={`Rated ${clamped.toFixed(1)} out of 5`}
    >
      <span className="star-rating-empty" aria-hidden="true">★★★★★</span>
      <span
        className="star-rating-fill"
        aria-hidden="true"
        style={{ width: `${fillPercent}%` }}
      >
        ★★★★★
      </span>
    </span>
  );
}
