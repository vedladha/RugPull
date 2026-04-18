import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import WishlistPage from "../WishlistPage.jsx"; // Adjust path if needed

const mockGetWishlistItems = vi.fn();
const mockRemoveFromWishlist = vi.fn();
const mockUseAuth = vi.fn();

vi.mock("../Auth/auth-context", () => ({
  useAuth: () => mockUseAuth(),
}));

const renderWishlistPage = () => render(
  <MemoryRouter>
    <WishlistPage />
  </MemoryRouter>,
);

describe("WishlistPage", () => {
  beforeEach(() => {
    mockGetWishlistItems.mockReset();
    mockRemoveFromWishlist.mockReset();
    mockUseAuth.mockReturnValue({
      user: { email: "test@example.com", displayName: "Test User" },
      getWishlistItems: mockGetWishlistItems,
      removeFromWishlist: mockRemoveFromWishlist,
      getUserRating: vi.fn().mockResolvedValue(null),
      createRating: vi.fn(),
      updateRating: vi.fn(),
      deleteRating: vi.fn(),
    });
  });

  it("shows sign-in message when there is no user", () => {
    mockUseAuth.mockReturnValue({
      user: null,
      getWishlistItems: mockGetWishlistItems,
      removeFromWishlist: mockRemoveFromWishlist,
      getUserRating: vi.fn().mockResolvedValue(null),
      createRating: vi.fn(),
      updateRating: vi.fn(),
      deleteRating: vi.fn(),
    });

    renderWishlistPage();

    expect(screen.getByText("Sign in to view your wishlist.")).toBeInTheDocument();
  });

  it("shows loading state initially", () => {
    mockGetWishlistItems.mockReturnValue(new Promise(() => {}));

    renderWishlistPage();

    expect(screen.getByText("Loading wishlist...")).toBeInTheDocument();
  });

  it("shows empty state when no items are returned", async () => {
    mockGetWishlistItems.mockResolvedValue([]);

    renderWishlistPage();

    await waitFor(() => {
      expect(screen.getByText("Your wishlist is empty.")).toBeInTheDocument();
    });
  });

  it("renders wishlist items after loading", async () => {
    mockGetWishlistItems.mockResolvedValue([
      { itemId: 1, name: "Guitar", description: "Great condition", price: 5.2, sellerName: "john" },
    ]);

    renderWishlistPage();

    await waitFor(() => {
      expect(screen.getByText("Guitar")).toBeInTheDocument();
      expect(screen.getByText("Seller: john")).toBeInTheDocument();
    });
  });

  it("opens the listing modal when a wishlist item is clicked", async () => {
    const guitarItem = { 
      itemId: 1, 
      name: "Guitar", 
      description: "Great condition", 
      price: 5.2, 
      sellerName: "john" 
    };

    mockGetWishlistItems.mockResolvedValue([guitarItem]);

    renderWishlistPage();

    // Wait for the card to appear
    const openButton = await screen.findByRole("button", { name: /view details for guitar/i });
    await userEvent.click(openButton);

    // The modal now instantly renders using the data we passed it (no internal fetch needed)
    const dialog = await screen.findByRole("dialog", { name: /guitar/i });
    
    expect(dialog).toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: /buy it now/i })).toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: /add to cart/i })).toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: /remove from wishlist/i })).toBeInTheDocument();
  });

  it("removes an item from the page after successful removal", async () => {
    mockGetWishlistItems.mockResolvedValue([
      { itemId: 1, name: "Guitar", description: "Great condition", price: 5.2, sellerName: "john" },
    ]);
    mockRemoveFromWishlist.mockResolvedValue({ message: "Item removed from wishlist", itemId: 1 });

    renderWishlistPage();

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

    renderWishlistPage();

    await waitFor(() => {
      expect(screen.getByText("Guitar")).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: "Remove from Wishlist" }));

    await waitFor(() => {
      expect(screen.getByText("Could not remove item")).toBeInTheDocument();
    });
  });
});