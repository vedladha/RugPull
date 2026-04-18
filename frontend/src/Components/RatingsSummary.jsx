import StarRating from "./StarRating.jsx";

export default function RatingsSummary({ average = 0, total = 0 }) {
  const avg = Number(average) || 0;
  const count = Number(total) || 0;

  return (
    <div className="ratings-summary">
      <div className="ratings-summary-label">Overall Rating</div>
      <StarRating value={avg} size="medium" />
      <div className="ratings-summary-count">
        {count === 0
          ? "No ratings yet"
          : `${count} rating${count === 1 ? "" : "s"}`}
      </div>
    </div>
  );
}
