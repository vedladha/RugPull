import StarRating from "./StarRating.jsx";

export default function RatingsSummary({ average = 0, total = 0 }) {
  const avg = Number(average) || 0;
  const count = Number(total) || 0;

  return (
    <div className="ratings-summary">
      <div className="ratings-summary-header">
        <StarRating value={avg} size="medium" />
        <span className="ratings-summary-average">{avg.toFixed(1)} out of 5</span>
      </div>
      <div className="ratings-summary-count">
        {count === 0
          ? "No ratings yet"
          : `${count} rating${count === 1 ? "" : "s"}`}
      </div>
    </div>
  );
}
