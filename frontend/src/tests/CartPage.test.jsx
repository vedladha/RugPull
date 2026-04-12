import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import CartPage from "../Pages/CartPage.jsx";

const mockUseAuth = vi.fn();
const mockFetch = vi.fn();
const mockNavigate = vi.fn();

vi.mock("../Auth/auth-context.js", () => ({
    useAuth: () => mockUseAuth(),
}));

// Mock ListingModal to isolate CartPage logic
vi.mock("react-router-dom", async () => {
    const actual = await vi.importActual("react-router-dom");
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock("../Components/ListingModal.jsx", () => ({
    default: ({ listing, onClose }) => (
        <div data-testid="listing-modal">
            <p>Modal: {listing.name}</p>
            <button onClick={onClose}>Close Modal</button>
        </div>
    ),
}));

describe("CartPage", () => {
    beforeEach(() => {
        vi.resetAllMocks();
        vi.stubGlobal("fetch", mockFetch);

        // Default: authenticated user
        mockUseAuth.mockReturnValue({
            user: { userId: 1, email: "test@example.com" }
        });
    });

    // Tests that sign-in prompt is shown when user is missing
    it("renders sign-in message when user is not authenticated", () => {
        mockUseAuth.mockReturnValue({ user: null });
        render(<CartPage />);
        expect(screen.getByText(/Please sign in to view your cart/i)).toBeDefined();
    });

    // Tests that empty cart message is shown
    it("shows empty cart message when cart is empty", async () => {
        mockFetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ cart: [] }),
        });

        render(<CartPage />);

        await waitFor(() => {
            expect(screen.getByText(/Your cart is currently empty/i)).toBeDefined();
        });
    });

    // Tests the data merging between /cart and /items/batch
    it("loads and merges cart data with item details from batch request", async () => {
        // Mock 1: /cart GET
        mockFetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                cart: [{ itemId: 101, quantity: 2 }]
            }),
        });

        // Mock 2: /items/batch POST (Structure matching itemsJson.items)
        mockFetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                items: [
                    { itemId: 101, name: "Laser Sword", price: 50.00 }
                ]
            }),
        });

        render(<CartPage />);

        await waitFor(() => {
            expect(screen.getByText("Laser Sword")).toBeDefined();
            expect(screen.getByText("$50.00")).toBeDefined();
            // Total should be 50 * 2 = 100
            const totals = screen.getAllByText("$100.00");
            expect(totals.length).toBeGreaterThan(0);
        });
    });

    // Tests quantity update interaction
    it("calls PUT endpoint and updates UI when incrementing quantity", async () => {
        mockFetch
            .mockResolvedValueOnce({ ok: true, json: async () => [{ itemId: 101, quantity: 1 }] })
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({ items: [{ itemId: 101, name: "Item", price: 10 }] })
            });

        render(<CartPage />);
        await waitFor(() => expect(screen.getByText("Item")).toBeDefined());

        // Mock PUT response
        mockFetch.mockResolvedValueOnce({ ok: true });

        const incrementBtn = screen.getByText("+");
        await userEvent.click(incrementBtn);

        await waitFor(() => {
            expect(mockFetch).toHaveBeenCalledWith(
                expect.stringContaining("/cart/101?quantity=2"),
                expect.objectContaining({ method: "PUT" })
            );
            expect(screen.getByText("2")).toBeDefined();
        });
    });

    // Tests item removal
    it("calls DELETE endpoint and removes item from list", async () => {
        mockFetch
            .mockResolvedValueOnce({ ok: true, json: async () => [{ itemId: 101, quantity: 1 }] })
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({ items: [{ itemId: 101, name: "Trash Item", price: 5 }] })
            });

        render(<CartPage />);
        await waitFor(() => expect(screen.getByText("Trash Item")).toBeDefined());

        // Mock DELETE response
        mockFetch.mockResolvedValueOnce({ ok: true });

        const removeBtn = screen.getByLabelText("Remove item");
        await userEvent.click(removeBtn);

        await waitFor(() => {
            expect(mockFetch).toHaveBeenCalledWith(
                expect.stringContaining("/cart/101"),
                expect.objectContaining({ method: "DELETE" })
            );
            expect(screen.queryByText("Trash Item")).toBeNull();
        });
    });

    // Tests Modal interaction
    it("opens ListingModal when item info is clicked", async () => {
        mockFetch
            .mockResolvedValueOnce({ ok: true, json: async () => [{ itemId: 101, quantity: 1 }] })
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({ items: [{ itemId: 101, name: "Clickable Item", price: 5 }] })
            });

        render(<CartPage />);
        await waitFor(() => expect(screen.getByText("Clickable Item")).toBeDefined());

        // Click the item name (part of cart-item-info)
        const itemInfo = screen.getByText("Clickable Item");
        await userEvent.click(itemInfo);

        expect(screen.getByTestId("listing-modal")).toBeDefined();
        expect(screen.getByText("Modal: Clickable Item")).toBeDefined();

        // Close modal
        await userEvent.click(screen.getByText("Close Modal"));
        expect(screen.queryByTestId("listing-modal")).toBeNull();
    });

    // Tests error handling
    it("displays error message when the initial fetch fails", async () => {
        mockFetch.mockResolvedValueOnce({
            ok: false,
            status: 404
        });

        render(<CartPage />);

        await waitFor(() => {
            expect(screen.getByText(/Failed to fetch cart data/i)).toBeDefined();
        });
    });

    it("navigates to the order page with cart items", async () => {
        mockFetch
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({ cart: [{ itemId: 3, quantity: 2 }] }),
            })
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    items: [{ itemId: 3, name: "Checkout Item", price: 9.5, stock: 4 }],
                }),
            });

        render(<CartPage />);
        await waitFor(() => expect(screen.getByText("Checkout Item")).toBeDefined());

        await userEvent.click(screen.getByRole("button", { name: "Proceed to Checkout" }));

        expect(mockNavigate).toHaveBeenCalledWith("/order", {
            state: {
                source: "cart",
                items: [
                    expect.objectContaining({
                        itemId: 3,
                        quantity: 2,
                        fromCart: true,
                    }),
                ],
            },
        });
    });
});