import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import Listings from "../Listings.jsx";

const mockUseAuth = vi.fn();

vi.mock("../Auth/auth-context", () => ({
    useAuth: () => mockUseAuth(),
}));

const mockListings = [
    { itemId: 1, name: "Guitar", description: "Great condition", price: 5.2, sellerName: "john" },
    { itemId: 2, name: "Bike", description: "Barely used", price: 10.0, sellerName: "jane" },
];

beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
    mockUseAuth.mockReturnValue({
        user: null,
        getWishlist: vi.fn(),
        addToWishlist: vi.fn(),
        removeFromWishlist: vi.fn(),
    });
});

function renderListings() {
    return render(
        <MemoryRouter>
            <Listings />
        </MemoryRouter>,
    );
}

describe("Listings", () => {
    // Tests that loading state is shown initially
    it("shows loading state initially", async () => {
        let resolve;
        vi.stubGlobal("fetch", vi.fn(() => new Promise((r) => { resolve = r; }))); // never resolves
        renderListings();
        expect(screen.getByText("Loading listings...")).toBeInTheDocument();

        resolve({ ok: true, json: () => Promise.resolve({ items: [] })})
        await waitFor(() => expect(screen.queryByText("Loading listings...")).toBeNull());
    });

    // Tests that listings are displayed after fetch
    it("displays listings after fetch", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        renderListings();

        await waitFor(() => {
            expect(screen.getByText("Guitar")).toBeInTheDocument();
            expect(screen.getByText("Bike")).toBeInTheDocument();
        });

    });

    // Tests that error state is shown when fetch fails
    it("shows error state when fetch fails", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: false,
            status: 500,
        }));

        renderListings();

        await waitFor(() => {
            expect(screen.getByText(/Error loading listings/)).toBeInTheDocument();
        });
    });

    // Tests that no listings message is shown when list is empty
    it("shows no listings message when list is empty", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: [] }),
        }));

        renderListings();

        await waitFor(() => {
            expect(screen.getByText("No listings found matching your filters.")).toBeInTheDocument();
        });
    });

    // Tests that min price filter works
    it("filters listings by min price", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        renderListings();
        await waitFor(() => expect(screen.getByText("Guitar")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("0"), "8");

        await waitFor(() => {
            expect(screen.queryByText("Guitar")).toBeNull();
            expect(screen.getByText("Bike")).toBeInTheDocument();
        });
    });

    // Tests that max price filter works
    it("filters listings by max price", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        renderListings();
        await waitFor(() => expect(screen.getByText("Guitar")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("No limit"), "7");

        await waitFor(() => {
            expect(screen.getByText("Guitar")).toBeInTheDocument();
            expect(screen.queryByText("Bike")).toBeNull();
        });
    });

    // Tests that keyword filter works
    it("filters listings by keyword", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        renderListings();
        await waitFor(() => expect(screen.getByText("Guitar")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("Search listings..."), "Guitar");

        await waitFor(() => {
            expect(screen.getByText("Guitar")).toBeInTheDocument();
            expect(screen.queryByText("Bike")).toBeNull();
        });
    });

    it("opens and closes a listing details modal when a listing is clicked", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        renderListings();
        await waitFor(() => expect(screen.getByText("Guitar")).toBeInTheDocument());

        await userEvent.click(screen.getByRole("button", { name: /view details for guitar/i }));

        expect(screen.getByRole("dialog", { name: "Guitar" })).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Buy It Now" })).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Add to Cart" })).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Add to Wishlist" })).toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", { name: /close listing details/i }));

        await waitFor(() => {
            expect(screen.queryByRole("dialog")).toBeNull();
        });
    });

    it("adds an item to wishlist from the modal for a logged-in user", async () => {
        const addToWishlist = vi.fn().mockResolvedValue({ itemId: 1 });
        const getWishlist = vi.fn().mockResolvedValue([]);
        mockUseAuth.mockReturnValue({
            user: { email: "test@example.com" },
            getWishlist,
            addToWishlist,
            removeFromWishlist: vi.fn(),
        });

        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        render(<Listings />);
        await waitFor(() => expect(screen.getByText("Guitar")).toBeInTheDocument());
        await waitFor(() => expect(getWishlist).toHaveBeenCalled());

        await userEvent.click(screen.getByRole("button", { name: /view details for guitar/i }));
        await userEvent.click(screen.getByRole("button", { name: "Add to Wishlist" }));

        await waitFor(() => {
            expect(addToWishlist).toHaveBeenCalledWith(1);
            expect(screen.getByText("Item added to wishlist")).toBeInTheDocument();
        });
    });

    it("removes an item from wishlist from the modal when already saved", async () => {
        const removeFromWishlist = vi.fn().mockResolvedValue({
            message: "Item removed from wishlist",
            itemId: 1,
        });
        const getWishlist = vi.fn().mockResolvedValue([{ userId: 1, itemId: 1 }]);
        mockUseAuth.mockReturnValue({
            user: { email: "test@example.com" },
            getWishlist,
            addToWishlist: vi.fn(),
            removeFromWishlist,
        });

        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        render(<Listings />);
        await waitFor(() => expect(screen.getByText("Guitar")).toBeInTheDocument());
        await waitFor(() => expect(getWishlist).toHaveBeenCalled());

        await userEvent.click(screen.getByRole("button", { name: /view details for guitar/i }));
        await waitFor(() => {
            expect(screen.getByRole("button", { name: "Remove from Wishlist" }))
                .toBeInTheDocument();
        });

        await userEvent.click(screen.getByRole("button", { name: "Remove from Wishlist" }));

        await waitFor(() => {
            expect(removeFromWishlist).toHaveBeenCalledWith(1);
            expect(screen.getByText("Item removed from wishlist")).toBeInTheDocument();
        });
    });

    it("shows a sign-in message when trying to wishlist without a user", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        render(<Listings />);
        await waitFor(() => expect(screen.getByText("Guitar")).toBeInTheDocument());

        await userEvent.click(screen.getByRole("button", { name: /view details for guitar/i }));
        await userEvent.click(screen.getByRole("button", { name: "Add to Wishlist" }));

        await waitFor(() => {
            expect(screen.getByText("Sign in to save items to your wishlist.")).toBeInTheDocument();
        });
    });
});
