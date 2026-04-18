import { useState } from "react";

export default function RatingInput({
  value = 0,
  onSubmit,
  onDelete,
  busy = false,
  error = "",
  success = "",
}) {
  const [hovered, setHovered] = useState(0);
  const [selected, setSelected] = useState(Number(value) || 0);

  const displayValue = hovered || selected;
  const hasExisting = Number(value) > 0;

  const handleSubmit = () => {
    if (selected < 1 || selected > 5) return;
    onSubmit(selected);
  };

  return (
    <div className="rating-input">
      <div className="rating-input-label">
        {hasExisting ? "Your rating" : "Rate this item"}
      </div>
      <div
        className="rating-input-stars"
        onMouseLeave={() => setHovered(0)}
        role="radiogroup"
        aria-label="Rating"
      >
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            className={`rating-input-star ${star <= displayValue ? "rating-input-star-filled" : ""}`}
            onClick={() => setSelected(star)}
            onMouseEnter={() => setHovered(star)}
            disabled={busy}
            role="radio"
            aria-checked={selected === star}
            aria-label={`${star} star${star === 1 ? "" : "s"}`}
          >
            ★
          </button>
        ))}
      </div>

      {error && (
        <div className="rating-input-feedback rating-input-feedback-error" role="alert">
          {error}
        </div>
      )}
      {success && (
        <div className="rating-input-feedback rating-input-feedback-success" role="status">
          {success}
        </div>
      )}

      <div className="rating-input-actions">
        <button
          type="button"
          className="listing-action-btn listing-action-btn-primary"
          onClick={handleSubmit}
          disabled={busy || selected < 1}
        >
          {busy ? "Saving..." : hasExisting ? "Update Rating" : "Submit Rating"}
        </button>
        {hasExisting && (
          <button
            type="button"
            className="listing-action-btn listing-action-btn-secondary"
            onClick={onDelete}
            disabled={busy}
          >
            Remove Rating
          </button>
        )}
      </div>
    </div>
  );
}
