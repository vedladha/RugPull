import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import CartPage from "../Pages/CartPage.jsx";

const mockUseAuth = vi.fn();
const mockFetch = vi.fn();

vi.mock("../Auth/auth-context.js", () => ({
    useAuth: () => mockUseAuth(),
}));

vi.mock("../Components/ListingModal.jsx", () => ({
    default: ({ listing, onClose }) => (
        <div data-testid="listing-modal">
            <p>Modal: {listing.name}</p>
            <button onClick={onClose}>Close</button>
        </div>
    ),
}));

describe("CartPage", () => {
    beforeEach(() => {
        vi.resetAllMocks();
        // Use stubGlobal to mock fetch without using the 'global' keyword
        vi.stubGlobal("fetch", mockFetch);

        mockUseAuth.mockReturnValue({
            user: { userId: 1, email: "test@example.com" }
        });
    });

    it("loads and merges cart data with item details", async () => {
        // 1. Mock the /cart response
        mockFetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                cart: [{ itemId: 1, quantity: 2 }]
            }),
        });

        // 2. Mock the /items/batch response (matching your nested backend format)
        mockFetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                items: {
                    items: [{ itemId: 1, name: "Test Listing", price: 10.50 }]
                }
            }),
        });

        render(<CartPage />);

        // Wait for the name to appear
        await waitFor(() => {
            expect(screen.getByText("Test Listing")).toBeDefined();
        });

        // Verify the individual item price
        expect(screen.getByText("$10.50")).toBeDefined();

        // Verify the totals. Use getAllByText because $21.00 appears in:
        // 1. The item row total
        // 2. The order subtotal
        // 3. The order total
        const totalElements = screen.getAllByText("$21.00");
        expect(totalElements.length).toBeGreaterThanOrEqual(1);
        
        // Verify quantity is displayed
        expect(screen.getByText("2")).toBeDefined();
    });

    it("displays error message when /cart fetch fails", async () => {
        mockFetch.mockResolvedValueOnce({
            ok: false,
            status: 500
        });

        render(<CartPage />);

        await waitFor(() => {
            expect(screen.getByText(/Failed to fetch cart data/i)).toBeDefined();
        });
    });

    it("renders sign-in message when user is not authenticated", () => {
        mockUseAuth.mockReturnValue({ user: null });
        render(<CartPage />);
        expect(screen.getByText(/Please sign in to view your cart/i)).toBeDefined();
    });

    it("calls DELETE endpoint and removes item from UI", async () => {
        mockFetch
            .mockResolvedValueOnce({ ok: true, json: async () => ({ cart: [{ itemId: 1, quantity: 1 }] }) })
            .mockResolvedValueOnce({ ok: true, json: async () => ({ items: { items: [{ itemId: 1, name: "Delete Me", price: 5 }] } }) });

        render(<CartPage />);
        await waitFor(() => expect(screen.getByText("Delete Me")).toBeDefined());

        // Mock the DELETE success
        mockFetch.mockResolvedValueOnce({ ok: true });

        const removeBtn = screen.getByLabelText("Remove item");
        await userEvent.click(removeBtn);

        await waitFor(() => {
            expect(screen.queryByText("Delete Me")).toBeNull();
        });
    });
});