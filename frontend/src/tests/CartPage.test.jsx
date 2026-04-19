import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom"; // <-- Required for useNavigate
import CartPage from "../Pages/CartPage.jsx";

const mockUseAuth = vi.fn();
const mockFetch = vi.fn();
const mockNavigate = vi.fn();

vi.mock("../Auth/auth-context", () => ({
    useAuth: () => mockUseAuth(),
}));

// Mock react-router-dom to safely spy on navigation
vi.mock("react-router-dom", async (importOriginal) => {
    const actual = await importOriginal();
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

// Mock ListingModal to isolate CartPage logic
vi.mock("../Components/ListingModal.jsx", () => ({
    default: ({ listing, onClose }) => (
        <div data-testid="listing-modal">
            <p>Modal: {listing.name}</p>
            <button onClick={onClose}>Close Modal</button>
        </div>
    ),
}));

// Helper function to ALWAYS wrap CartPage in a MemoryRouter
const renderCartPage = () => {
    return render(
        <MemoryRouter>
            <CartPage />
        </MemoryRouter>
    );
};

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
        renderCartPage();
        
        // UPDATED: Now gracefully matches the new SignInPrompt component
        expect(screen.getByRole('heading', { name: "View Your Cart" })).toBeInTheDocument();
        expect(screen.getByText("Sign in to view your cart")).toBeInTheDocument();
    });

    // Tests that empty cart message is shown
    it("shows empty cart message when cart is empty", async () => {
        mockFetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ cart: [] }),
        });

        renderCartPage();

        await waitFor(() => {
            expect(screen.getByText(/Your cart is currently empty/i)).toBeInTheDocument();
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
                    { itemId: 101, name: "Laser Sword", price: 50.00, stock: 5 }
                ]
            }),
        });

        renderCartPage();

        await waitFor(() => {
            expect(screen.getByText("Laser Sword")).toBeInTheDocument();
            expect(screen.getByText("$50.00")).toBeInTheDocument();
            expect(screen.getByText("Qty in cart: 2")).toBeInTheDocument();
            expect(screen.getByText("5 in stock")).toBeInTheDocument();
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
                json: async () => ({ items: [{ itemId: 101, name: "Item", price: 10, stock: 3 }] })
            });

        renderCartPage();
        await waitFor(() => expect(screen.getByText("Item")).toBeInTheDocument());

        // Mock PUT response
        mockFetch.mockResolvedValueOnce({ ok: true });

        const incrementBtn = screen.getByText("+");
        await userEvent.click(incrementBtn);

        await waitFor(() => {
            expect(mockFetch).toHaveBeenCalledWith(
                expect.stringContaining("/cart/101?quantity=2"),
                expect.objectContaining({ method: "PUT" })
            );
            expect(screen.getByText("2")).toBeInTheDocument();
        });
    });

    // Tests item removal
    it("calls DELETE endpoint and removes item from list", async () => {
        mockFetch
            .mockResolvedValueOnce({ ok: true, json: async () => [{ itemId: 101, quantity: 1 }] })
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({ items: [{ itemId: 101, name: "Trash Item", price: 5, stock: 2 }] })
            });

        renderCartPage();
        await waitFor(() => expect(screen.getByText("Trash Item")).toBeInTheDocument());

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
                json: async () => ({ items: [{ itemId: 101, name: "Clickable Item", price: 5, stock: 2 }] })
            });

        renderCartPage();
        await waitFor(() => expect(screen.getByText("Clickable Item")).toBeInTheDocument());

        // Click the item name (part of cart-item-info)
        const itemInfo = screen.getByText("Clickable Item");
        await userEvent.click(itemInfo);

        expect(screen.getByTestId("listing-modal")).toBeInTheDocument();
        expect(screen.getByText("Modal: Clickable Item")).toBeInTheDocument();

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

        renderCartPage();

        await waitFor(() => {
            expect(screen.getByText(/Failed to fetch cart data/i)).toBeInTheDocument();
        });
    });

    it("disables quantity increase when the cart quantity matches stock", async () => {
        mockFetch
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({ cart: [{ itemId: 8, quantity: 2 }] }),
            })
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    items: [{ itemId: 8, name: "Limited Item", price: 12, stock: 2 }],
                }),
            });

        renderCartPage();
        await waitFor(() => expect(screen.getByText("Limited Item")).toBeInTheDocument());

        const increaseButton = screen.getByRole("button", { name: "Increase quantity for Limited Item" });
        expect(increaseButton).toBeDisabled();

        await userEvent.click(increaseButton);

        expect(mockFetch).toHaveBeenCalledTimes(2);
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

        renderCartPage();
        await waitFor(() => expect(screen.getByText("Checkout Item")).toBeInTheDocument());

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