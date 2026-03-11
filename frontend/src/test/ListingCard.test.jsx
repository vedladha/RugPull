import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import ListingCard from "../Components/ListingCard.jsx";

describe("ListingCard", () => {
  const defaultProps = {
    title: "Guitar",
    bio: "Great condition, barely used",
    price: "5.2 RPC",
    seller: "john",
  };

  it("renders the title", () => {
    render(<ListingCard {...defaultProps} />);
    expect(screen.getByText("Guitar")).toBeInTheDocument();
  });

  it("renders the bio", () => {
    render(<ListingCard {...defaultProps} />);
    expect(screen.getByText("Great condition, barely used")).toBeInTheDocument();
  });

  it("renders the price", () => {
    render(<ListingCard {...defaultProps} />);
    expect(screen.getByText("5.2 RPC")).toBeInTheDocument();
  });

  it("renders the seller", () => {
    render(<ListingCard {...defaultProps} />);
    expect(screen.getByText("Seller: john")).toBeInTheDocument();
  });

  it("renders all props together correctly", () => {
    render(<ListingCard {...defaultProps} />);
    expect(screen.getByText("Guitar")).toBeInTheDocument();
    expect(screen.getByText("Great condition, barely used")).toBeInTheDocument();
    expect(screen.getByText("5.2 RPC")).toBeInTheDocument();
    expect(screen.getByText("Seller: john")).toBeInTheDocument();
  });
});