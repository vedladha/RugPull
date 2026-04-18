import StarRating from "./StarRating.jsx";

export default function RatingsSummary({ average = 0, total = 0, distribution = {} }) {
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
      {count > 0 && (
        <ul className="ratings-distribution">
          {[5, 4, 3, 2, 1].map((star) => {
            const starCount = Number(distribution?.[star]) || 0;
            const percent = count === 0 ? 0 : (starCount / count) * 100;
            return (
              <li key={star} className="ratings-distribution-row">
                <span className="ratings-distribution-label">{star} star</span>
                <span
                  className="ratings-distribution-bar"
                  role="progressbar"
                  aria-valuenow={starCount}
                  aria-valuemin={0}
                  aria-valuemax={count}
                  aria-label={`${starCount} ${star}-star rating${starCount === 1 ? "" : "s"}`}
                >
                  <span
                    className="ratings-distribution-bar-fill"
                    style={{ width: `${percent}%` }}
                  />
                </span>
                <span className="ratings-distribution-count">{starCount}</span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
