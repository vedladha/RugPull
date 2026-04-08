import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import WishlistPage from "../WishlistPage.jsx";

const mockGetWishlistItems = vi.fn();
const mockRemoveFromWishlist = vi.fn();
const mockUseAuth = vi.fn();

vi.mock("../Auth/auth-context", () => ({
  useAuth: () => mockUseAuth(),
}));

describe("WishlistPage", () => {
  beforeEach(() => {
    mockGetWishlistItems.mockReset();
    mockRemoveFromWishlist.mockReset();
    mockUseAuth.mockReturnValue({
      user: { email: "test@example.com", displayName: "Test User" },
      getWishlistItems: mockGetWishlistItems,
      removeFromWishlist: mockRemoveFromWishlist,
    });
  });

  it("shows sign-in message when there is no user", () => {
    mockUseAuth.mockReturnValue({
      user: null,
      getWishlistItems: mockGetWishlistItems,
      removeFromWishlist: mockRemoveFromWishlist,
    });

    render(<WishlistPage />);

    expect(screen.getByText("Sign in to view your wishlist.")).toBeInTheDocument();
  });

  it("shows loading state initially", () => {
    mockGetWishlistItems.mockReturnValue(new Promise(() => {}));

    render(<WishlistPage />);

    expect(screen.getByText("Loading wishlist...")).toBeInTheDocument();
  });

  it("shows empty state when no items are returned", async () => {
    mockGetWishlistItems.mockResolvedValue([]);

    render(<WishlistPage />);

    await waitFor(() => {
      expect(screen.getByText("Your wishlist is empty.")).toBeInTheDocument();
    });
  });

  it("renders wishlist items after loading", async () => {
    mockGetWishlistItems.mockResolvedValue([
      { itemId: 1, name: "Guitar", description: "Great condition", price: 5.2, sellerName: "john" },
    ]);

    render(<WishlistPage />);

    await waitFor(() => {
      expect(screen.getByText("Guitar")).toBeInTheDocument();
      expect(screen.getByText("Seller: john")).toBeInTheDocument();
    });
  });

  it("opens the listing modal when a wishlist item is clicked", async () => {
    mockGetWishlistItems.mockResolvedValue([
      { itemId: 1, name: "Guitar", description: "Great condition", price: 5.2, sellerName: "john" },
    ]);

    render(<WishlistPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /view details for guitar/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: /view details for guitar/i }));

    await waitFor(() => {
      const dialog = screen.getByRole("dialog", { name: "Guitar" });
      expect(dialog).toBeInTheDocument();
      expect(within(dialog).getByRole("button", { name: "Buy" })).toBeInTheDocument();
      expect(within(dialog).getByRole("button", { name: "Add to Cart" })).toBeInTheDocument();
      expect(within(dialog).getByRole("button", { name: "Remove from Wishlist" }))
        .toBeInTheDocument();
    });
  });

  it("removes an item from the page after successful removal", async () => {
    mockGetWishlistItems.mockResolvedValue([
      { itemId: 1, name: "Guitar", description: "Great condition", price: 5.2, sellerName: "john" },
    ]);
    mockRemoveFromWishlist.mockResolvedValue({ message: "Item removed from wishlist", itemId: 1 });

    render(<WishlistPage />);

    await waitFor(() => {
      expect(screen.getByText("Guitar")).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: "Remove from Wishlist" }));

    await waitFor(() => {
      expect(mockRemoveFromWishlist).toHaveBeenCalledWith(1);
      expect(screen.getByText("Item removed from wishlist")).toBeInTheDocument();
      expect(screen.getByText("Your wishlist is empty.")).toBeInTheDocument();
    });
  });

  it("shows an error when removing an item fails", async () => {
    mockGetWishlistItems.mockResolvedValue([
      { itemId: 1, name: "Guitar", description: "Great condition", price: 5.2, sellerName: "john" },
    ]);
    mockRemoveFromWishlist.mockRejectedValue(new Error("Could not remove item"));

    render(<WishlistPage />);

    await waitFor(() => {
      expect(screen.getByText("Guitar")).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: "Remove from Wishlist" }));

    await waitFor(() => {
      expect(screen.getByText("Could not remove item")).toBeInTheDocument();
    });
  });
});
