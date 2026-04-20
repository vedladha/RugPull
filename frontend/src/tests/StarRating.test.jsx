import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import StarRating from "../Components/StarRating.jsx"; // Adjust path if needed

describe("StarRating", () => {
  it("renders with default props (value 0, size medium)", () => {
    render(<StarRating />);
    
    const container = screen.getByRole("img", { name: "Rated 0.0 out of 5" });
    expect(container).toBeInTheDocument();
    expect(container).toHaveClass("star-rating-medium");

    // Check width of filled stars
    const fillSpan = container.querySelector(".star-rating-fill");
    expect(fillSpan).toHaveStyle({ width: "0%" });
  });

  it("renders correct fill percentage and label for a valid rating", () => {
    render(<StarRating value={3.5} />);
    
    const container = screen.getByRole("img", { name: "Rated 3.5 out of 5" });
    expect(container).toBeInTheDocument();

    const fillSpan = container.querySelector(".star-rating-fill");
    // 3.5 / 5 = 0.7 = 70%
    expect(fillSpan).toHaveStyle({ width: "70%" }); 
  });

  it("clamps ratings above 5 down to 5", () => {
    render(<StarRating value={7.5} />);
    
    const container = screen.getByRole("img", { name: "Rated 5.0 out of 5" });
    expect(container).toBeInTheDocument();

    const fillSpan = container.querySelector(".star-rating-fill");
    expect(fillSpan).toHaveStyle({ width: "100%" });
  });

  it("clamps negative ratings up to 0", () => {
    render(<StarRating value={-2} />);
    
    const container = screen.getByRole("img", { name: "Rated 0.0 out of 5" });
    expect(container).toBeInTheDocument();

    const fillSpan = container.querySelector(".star-rating-fill");
    expect(fillSpan).toHaveStyle({ width: "0%" });
  });

  it("handles invalid non-numeric string inputs gracefully by defaulting to 0", () => {
    render(<StarRating value="not-a-number" />);
    
    const container = screen.getByRole("img", { name: "Rated 0.0 out of 5" });
    expect(container).toBeInTheDocument();
    
    const fillSpan = container.querySelector(".star-rating-fill");
    expect(fillSpan).toHaveStyle({ width: "0%" });
  });

  it("applies custom size class correctly", () => {
    render(<StarRating size="large" />);
    
    const container = screen.getByRole("img");
    expect(container).toHaveClass("star-rating-large");
  });
});