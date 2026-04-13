import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import SellPage from "../SellPage.jsx";

const mockUseAuth = vi.fn();

vi.mock("../Auth/auth-context", () => ({
    useAuth: () => mockUseAuth(),
}));

const loggedInUser = { email: "test@example.com", displayName: "TestUser" };

beforeEach(() => {
    mockUseAuth.mockReturnValue({ user: loggedInUser });
    vi.stubGlobal("fetch", vi.fn());
});

describe("SellPage", () => {
    // Tests that a message is shown when user is not signed in
    it("shows sign in message when user is not authenticated", () => {
        mockUseAuth.mockReturnValue({ user: null });
        render(<SellPage />);
        expect(screen.getByText("Please sign in to list an item for sale.")).toBeInTheDocument();
    });

    // Tests that the form is shown when user is signed in
    it("renders the form when user is signed in", () => {
        render(<SellPage />);
        expect(screen.getByPlaceholderText("What are you selling?")).toBeInTheDocument();
        expect(screen.getByPlaceholderText("Describe the condition, specs, or any relevant details...")).toBeInTheDocument();
        expect(screen.getByPlaceholderText("0.00")).toBeInTheDocument();
        expect(screen.getByLabelText("Quantity")).toBeInTheDocument();
    });

    // Tests that validation errors are shown when form is submitted empty
    it("shows validation errors when form is submitted empty", async () => {
        render(<SellPage />);
        await userEvent.click(screen.getByText("Post Listing"));

        expect(screen.getByText("Title is required")).toBeInTheDocument();
        expect(screen.getByText("Description is required")).toBeInTheDocument();
        expect(screen.getByText("Enter a valid price")).toBeInTheDocument();
    });

    // Tests that title error is cleared when title is typed
    it("clears title error when title is typed", async () => {
        render(<SellPage />);
        await userEvent.click(screen.getByText("Post Listing"));
        expect(screen.getByText("Title is required")).toBeInTheDocument();

        await userEvent.type(screen.getByPlaceholderText("What are you selling?"), "Guitar");
        expect(screen.queryByText("Title is required")).toBeNull();
    });

    // Tests that price error is shown when price is 0 or negative
    it("shows price error when price is 0", async () => {
        render(<SellPage />);
        await userEvent.type(screen.getByPlaceholderText("What are you selling?"), "Guitar");
        await userEvent.type(screen.getByPlaceholderText("Describe the condition, specs, or any relevant details..."), "Great condition");
        await userEvent.type(screen.getByPlaceholderText("0.00"), "0");
        await userEvent.click(screen.getByText("Post Listing"));

        expect(screen.getByText("Enter a valid price")).toBeInTheDocument();
    });

    it("shows quantity error when quantity is 0", async () => {
        render(<SellPage />);

        const quantityInput = screen.getByLabelText("Quantity");
        await userEvent.clear(quantityInput);
        await userEvent.type(quantityInput, "0");
        await userEvent.click(screen.getByText("Post Listing"));

        expect(screen.getByText("Enter a valid quantity")).toBeInTheDocument();
    });

    // Tests that a successful submission shows the success screen
    it("shows success screen after successful submission", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true }));
        render(<SellPage />);

        await userEvent.type(screen.getByPlaceholderText("What are you selling?"), "Guitar");
        await userEvent.type(screen.getByPlaceholderText("Describe the condition, specs, or any relevant details..."), "Great condition");
        await userEvent.type(screen.getByPlaceholderText("0.00"), "5.2");
        const quantityInput = screen.getByLabelText("Quantity");
        await userEvent.clear(quantityInput);
        await userEvent.type(quantityInput, "4");
        await userEvent.click(screen.getByText("Post Listing"));

        await waitFor(() => {
            expect(screen.getByText("Listing Posted")).toBeInTheDocument();
            expect(screen.getByText("Guitar")).toBeInTheDocument();
            expect(screen.getByText("4 available")).toBeInTheDocument();
        });
    });

    // Tests that fetch is called with the correct data
    it("calls fetch with correct data on submit", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true }));
        render(<SellPage />);

        await userEvent.type(screen.getByPlaceholderText("What are you selling?"), "Guitar");
        await userEvent.type(screen.getByPlaceholderText("Describe the condition, specs, or any relevant details..."), "Great condition");
        await userEvent.type(screen.getByPlaceholderText("0.00"), "5.2");
        const quantityInput = screen.getByLabelText("Quantity");
        await userEvent.clear(quantityInput);
        await userEvent.type(quantityInput, "4");
        await userEvent.click(screen.getByText("Post Listing"));

        await waitFor(() => {
            expect(vi.mocked(fetch)).toHaveBeenCalledWith(
                "http://localhost:3001/items",
                expect.objectContaining({
                    method: "POST",
                    body: JSON.stringify({
                        name: "Guitar",
                        description: "Great condition",
                        price: 5.2,
                        stock: 4,
                    }),
                })
            );
        });
    });

    // Tests that error banner is shown when fetch throws
    it("shows error banner when fetch fails", async () => {
        vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("Network error")));
        render(<SellPage />);

        await userEvent.type(screen.getByPlaceholderText("What are you selling?"), "Guitar");
        await userEvent.type(screen.getByPlaceholderText("Describe the condition, specs, or any relevant details..."), "Great condition");
        await userEvent.type(screen.getByPlaceholderText("0.00"), "5.2");
        await userEvent.click(screen.getByText("Post Listing"));

        await waitFor(() => {
            expect(screen.getByText("Failed to post listing. Please try again.")).toBeInTheDocument();
        });
    });

    // Tests that clicking Post Another resets the form
    it("resets form when Post Another is clicked", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true }));
        render(<SellPage />);

        await userEvent.type(screen.getByPlaceholderText("What are you selling?"), "Guitar");
        await userEvent.type(screen.getByPlaceholderText("Describe the condition, specs, or any relevant details..."), "Great condition");
        await userEvent.type(screen.getByPlaceholderText("0.00"), "5.2");
        await userEvent.click(screen.getByText("Post Listing"));

        await waitFor(() => expect(screen.getByText("Post Another")).toBeInTheDocument());
        await userEvent.click(screen.getByText("Post Another"));

        expect(screen.getByPlaceholderText("What are you selling?")).toBeInTheDocument();
    });

    // Tests that character count updates as user types
    it("updates character count as user types title", async () => {
        render(<SellPage />);
        await userEvent.type(screen.getByPlaceholderText("What are you selling?"), "Guitar");
        expect(screen.getByText("6/80")).toBeInTheDocument();
    });
});
