import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import ListingCard from "../Components/ListingCard.jsx";

describe("ListingCard", () => {
  const defaultProps = {
    name: "Guitar",
    description: "Great condition, barely used",
    price: "5.2 RPC",
    stock: 3,
    seller: "john",
  };

  it("renders the title", () => {
    render(<ListingCard {...defaultProps} />);
    expect(screen.getByText("Guitar")).toBeInTheDocument();
  });

  it("renders the bio", () => {
    render(<ListingCard {...defaultProps} />);
    expect(
      screen.getByText("Great condition, barely used"),
    ).toBeInTheDocument();
  });

  it("renders the price", () => {
    render(<ListingCard {...defaultProps} />);
    expect(screen.getByText("5.2 RPC")).toBeInTheDocument();
  });

  it("renders the seller", () => {
    render(<ListingCard {...defaultProps} />);
    expect(screen.getByText("Seller: john")).toBeInTheDocument();
  });

  it("renders the stock quantity", () => {
    render(<ListingCard {...defaultProps} />);
    expect(screen.getByText("Quantity available: 3")).toBeInTheDocument();
    expect(screen.getByText("3 left")).toBeInTheDocument();
  });

  it("renders all props together correctly", () => {
    render(<ListingCard {...defaultProps} />);
    expect(screen.getByText("Guitar")).toBeInTheDocument();
    expect(
      screen.getByText("Great condition, barely used"),
    ).toBeInTheDocument();
    expect(screen.getByText("5.2 RPC")).toBeInTheDocument();
    expect(screen.getByText("Quantity available: 3")).toBeInTheDocument();
    expect(screen.getByText("Seller: john")).toBeInTheDocument();
  });

  it("calls onClick when card is clicked", async () => {
    const onClick = vi.fn();
    render(<ListingCard {...defaultProps} onClick={onClick} />);

    await userEvent.click(
      screen.getByRole("button", { name: /view details for guitar/i }),
    );

    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
