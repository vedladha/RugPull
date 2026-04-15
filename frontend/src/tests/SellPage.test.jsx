import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import SellPage from "../Pages/SellPage.jsx"; // Adjust path as needed

const mockUseAuth = vi.fn();

vi.mock("../Auth/auth-context", () => ({
    useAuth: () => mockUseAuth(),
}));

const loggedInUser = { email: "test@example.com", displayName: "TestUser", userId: 1 };

describe("SellPage", () => {
    beforeEach(() => {
        vi.resetAllMocks();
        mockUseAuth.mockReturnValue({ user: loggedInUser });
        
        // Mock the initial GET /items/me request to return an empty inventory by default
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: [] })
        }));

        // Mock window.confirm for the delete tests
        window.confirm = vi.fn().mockReturnValue(true);

        // Mock scrollIntoView which is used when clicking "Edit"
        window.HTMLElement.prototype.scrollIntoView = vi.fn();
    });

    // 1. Unauthenticated View
    it("shows sign in message when user is not authenticated", () => {
        mockUseAuth.mockReturnValue({ user: null });
        render(<SellPage />);
        expect(screen.getByText("Please sign in to manage your listings.")).toBeInTheDocument();
    });

    // 2. Initial Render & Fetch
    it("renders the form and empty inventory when user is signed in", async () => {
        render(<SellPage />);
        
        // Wait for the on-mount fetch to complete
        await waitFor(() => expect(screen.queryByText("Loading inventory...")).not.toBeInTheDocument());

        expect(screen.getByPlaceholderText("What are you selling?")).toBeInTheDocument();
        expect(screen.getByText("You don't have any active listings yet.")).toBeInTheDocument();
    });

    // 3. Validation Errors
    it("shows validation errors when form is submitted empty", async () => {
        render(<SellPage />);
        await waitFor(() => expect(screen.queryByText("Loading inventory...")).not.toBeInTheDocument());
        
        await userEvent.click(screen.getByText("Post Listing"));

        expect(screen.getByText("Title is required")).toBeInTheDocument();
        expect(screen.getByText("Description is required")).toBeInTheDocument();
        expect(screen.getByText("Enter a valid price")).toBeInTheDocument();
    });

    // 4. Quantity Validation (Updated to check for negative numbers, since 0 is now allowed)
    it("shows quantity error when quantity is negative", async () => {
        render(<SellPage />);
        await waitFor(() => expect(screen.queryByText("Loading inventory...")).not.toBeInTheDocument());

        const quantityInput = screen.getByLabelText("Quantity");
        await userEvent.clear(quantityInput);
        await userEvent.type(quantityInput, "-1");
        await userEvent.click(screen.getByText("Post Listing"));

        expect(screen.getByText("Enter a valid quantity")).toBeInTheDocument();
    });

    // 5. Successful POST (New Listing)
    it("submits a new listing, calls POST, and shows inline success", async () => {
        const fetchMock = vi.fn()
            // 1st call: on-mount GET
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ items: [] }) })
            // 2nd call: form submission POST
            .mockResolvedValueOnce({ 
                ok: true, 
                json: () => Promise.resolve({ 
                    item: { itemId: 99, name: "Guitar", price: 5.2, stock: 4 } 
                }) 
            });
        
        vi.stubGlobal("fetch", fetchMock);
        render(<SellPage />);
        await waitFor(() => expect(screen.queryByText("Loading inventory...")).not.toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("What are you selling?"), "Guitar");
        await userEvent.type(screen.getByPlaceholderText("Describe the condition, specs, or any relevant details..."), "Great condition");
        await userEvent.type(screen.getByPlaceholderText("0.00"), "5.2");
        const quantityInput = screen.getByLabelText("Quantity");
        await userEvent.clear(quantityInput);
        await userEvent.type(quantityInput, "4");
        
        await userEvent.click(screen.getByText("Post Listing"));

        await waitFor(() => {
            // Check success banner
            expect(screen.getByText("New listing successfully posted!")).toBeInTheDocument();
            // Verify POST request
            expect(fetchMock).toHaveBeenCalledWith("http://localhost:3001/items", expect.objectContaining({
                method: "POST",
                body: JSON.stringify({ name: "Guitar", description: "Great condition", price: 5.2, stock: 4 })
            }));
            // Verify item was added to the inventory list at the bottom
            expect(screen.getByRole('heading', { name: "Guitar" })).toBeInTheDocument();
        });
    });

    // 6. Successful PATCH (Editing a Listing)
    it("loads item into form on Edit click, and sends a partial PATCH request", async () => {
        const mockItem = { itemId: 42, name: "Old Camera", description: "Works fine", price: 100, stock: 1 };
        
        const fetchMock = vi.fn()
            // 1st call: on-mount GET (returns 1 item)
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ items: [mockItem] }) })
            // 2nd call: form submission PATCH
            .mockResolvedValueOnce({ 
                ok: true, 
                json: () => Promise.resolve({ 
                    item: { ...mockItem, price: 80 } // Price updated
                }) 
            });
        
        vi.stubGlobal("fetch", fetchMock);
        render(<SellPage />);
        await waitFor(() => expect(screen.queryByText("Loading inventory...")).not.toBeInTheDocument());

        // Click Edit on the inventory item
        await userEvent.click(screen.getByRole("button", { name: "Edit" }));

        // Verify form changed to edit mode
        expect(screen.getByText("Edit Listing")).toBeInTheDocument();
        const priceInput = screen.getByPlaceholderText("0.00");
        expect(priceInput.value).toBe("100");

        // Change the price
        await userEvent.clear(priceInput);
        await userEvent.type(priceInput, "80");

        // Submit changes
        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            // Check success banner
            expect(screen.getByText("Listing successfully updated!")).toBeInTheDocument();
            // Verify PATCH request ONLY contains the changed field (price)
            expect(fetchMock).toHaveBeenCalledWith("http://localhost:3001/items/42", expect.objectContaining({
                method: "PATCH",
                body: JSON.stringify({ price: 80 })
            }));
        });
    });

    // 7. Prevent Empty PATCH
    it("aborts PATCH request if no changes were made during edit", async () => {
        const mockItem = { itemId: 42, name: "Old Camera", description: "Works fine", price: 100, stock: 1 };
        const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve({ items: [mockItem] }) });
        vi.stubGlobal("fetch", fetchMock);
        
        render(<SellPage />);
        await waitFor(() => expect(screen.queryByText("Loading inventory...")).not.toBeInTheDocument());

        await userEvent.click(screen.getByRole("button", { name: "Edit" }));
        await userEvent.click(screen.getByText("Save Changes")); // Save without touching anything

        await waitFor(() => {
            expect(screen.getByText("No changes were made.")).toBeInTheDocument();
            // Verify fetch was only called ONCE (the initial GET), meaning the PATCH was aborted
            expect(fetchMock).toHaveBeenCalledTimes(1); 
        });
    });

    // 8. Delete Request
    it("calls DELETE and removes item from inventory list", async () => {
        const mockItem = { itemId: 42, name: "Old Camera", description: "Works fine", price: 100, stock: 1 };
        
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ items: [mockItem] }) })
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({}) }); // DELETE response
            
        vi.stubGlobal("fetch", fetchMock);
        render(<SellPage />);
        await waitFor(() => expect(screen.queryByText("Loading inventory...")).not.toBeInTheDocument());

        await userEvent.click(screen.getByRole("button", { name: "Delete" }));

        await waitFor(() => {
            expect(window.confirm).toHaveBeenCalled();
            expect(fetchMock).toHaveBeenCalledWith("http://localhost:3001/items/42", expect.objectContaining({ method: "DELETE" }));
            expect(screen.getByText("You don't have any active listings yet.")).toBeInTheDocument();
        });
    });

    // 9. Backend Error Handling
    it("surfaces exact backend error message when submission fails", async () => {
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ items: [] }) })
            .mockResolvedValueOnce({ 
                ok: false, 
                json: () => Promise.resolve({ error: "Backend validation failed: name cannot contain symbols" }) 
            });
        
        vi.stubGlobal("fetch", fetchMock);
        render(<SellPage />);
        await waitFor(() => expect(screen.queryByText("Loading inventory...")).not.toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("What are you selling?"), "Guitar @#$");
        await userEvent.type(screen.getByPlaceholderText("Describe the condition, specs, or any relevant details..."), "Great");
        await userEvent.type(screen.getByPlaceholderText("0.00"), "50");
        await userEvent.click(screen.getByText("Post Listing"));

        await waitFor(() => {
            expect(screen.getByText("Backend validation failed: name cannot contain symbols")).toBeInTheDocument();
        });
    });
});