import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import EarnPage from "../Pages/EarnPage.jsx"; // Adjust path if necessary

const mockUseAuth = vi.fn();

vi.mock("../Auth/auth-context", () => ({
    useAuth: () => mockUseAuth(),
}));

describe("EarnPage", () => {
    beforeEach(() => {
        vi.stubGlobal("fetch", vi.fn());
        // Default auth state for most tests
        mockUseAuth.mockReturnValue({
            user: { email: "test@example.com", id: 1 },
        });
    });

    // 1. Authentication View
    it("shows logged out message when no user is present", () => {
        mockUseAuth.mockReturnValue({ user: null });
        render(<EarnPage />);
        
        expect(screen.getByText("Please sign in to view your rewards.")).toBeInTheDocument();
        expect(screen.queryByText("Checking reward status...")).toBeNull();
    });

    // 2. Loading State
    it("shows loading state initially while fetching daily status", async () => {
        let resolve;
        vi.stubGlobal("fetch", vi.fn(() => new Promise((r) => { resolve = r; }))); // never resolves initially
        
        render(<EarnPage />);
        
        expect(screen.getByText("Checking reward status...")).toBeInTheDocument();

        resolve({ ok: true, json: () => Promise.resolve({ status: { claimed: true } }) });
        await waitFor(() => expect(screen.queryByText("Checking reward status...")).toBeNull());
    });

    // 3. Display Available Claim
    it("displays claim banner with streak and reward amount when claim is available", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ 
                status: { claimed: false, streak: 3, next_reward_amount: 14.0 } 
            }),
        }));

        render(<EarnPage />);

        await waitFor(() => {
            expect(screen.getByText("🔥 3 Day Streak")).toBeInTheDocument();
            expect(screen.getByText("Check in today to claim your free 14.0 $RPC tokens!")).toBeInTheDocument();
            expect(screen.getByRole("button", { name: "Claim Tokens" })).toBeInTheDocument();
        });
    });

    // 4. Display Already Claimed
    it("displays already claimed message and current streak when previously claimed", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ 
                status: { claimed: true, streak: 5 } 
            }),
        }));

        render(<EarnPage />);

        await waitFor(() => {
            expect(screen.getByText("You have already claimed your reward today. Come back tomorrow!")).toBeInTheDocument();
            expect(screen.getByText("🔥 Current Streak: 5 Days")).toBeInTheDocument();
            expect(screen.queryByRole("button", { name: "Claim Tokens" })).toBeNull();
        });
    });

    // 5. Successful Claim Action
    it("handles claim success and optimistically updates streak", async () => {
        const fetchMock = vi.fn()
            // First call: initial page load
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({ 
                    status: { claimed: false, streak: 2, next_reward_amount: 10.0 } 
                }),
            })
            // Second call: clicking the claim button
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({}),
            });
        
        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);

        await waitFor(() => expect(screen.getByRole("button", { name: "Claim Tokens" })).toBeInTheDocument());

        await userEvent.click(screen.getByRole("button", { name: "Claim Tokens" }));

        // Check optimistic UI update: streak jumps from 2 to 3, and banner disappears
        await waitFor(() => {
            expect(screen.getByText("🔥 Current Streak: 3 Days")).toBeInTheDocument();
            expect(screen.queryByRole("button", { name: "Claim Tokens" })).toBeNull();
        });
    });

    // 6. Failed Claim Action
    it("shows error when claiming fails", async () => {
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({ 
                    status: { claimed: false, streak: 0, next_reward_amount: 10.0 } 
                }),
            })
            .mockResolvedValueOnce({
                ok: false,
                json: () => Promise.resolve({ error: "Network error occurred" }),
            });
        
        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);

        await waitFor(() => expect(screen.getByRole("button", { name: "Claim Tokens" })).toBeInTheDocument());

        await userEvent.click(screen.getByRole("button", { name: "Claim Tokens" }));

        await waitFor(() => {
            expect(screen.getByText("Error: Network error occurred")).toBeInTheDocument();
        });
    });

    // 7. Dev Sandbox Validation
    it("shows error when dev mint amount is invalid or zero", async () => {
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ status: { claimed: true } }),
        });
        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);

        await waitFor(() => expect(screen.getByPlaceholderText("Amount (e.g. 500)")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("Amount (e.g. 500)"), "-50");
        await userEvent.click(screen.getByRole("button", { name: "Execute Mint" }));

        expect(screen.getByText("Please enter a valid amount greater than 0.")).toBeInTheDocument();
        
        // Assert fetch was only called ONCE (the initial page load), preventing the bad POST request
        expect(fetchMock).toHaveBeenCalledTimes(1); 
    });

    // 8. Dev Sandbox Success
    it("successfully mints tokens via the developer tool", async () => {
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({ status: { claimed: true } }),
            })
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({}),
            });
        
        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);

        await waitFor(() => expect(screen.getByPlaceholderText("Amount (e.g. 500)")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("Amount (e.g. 500)"), "150");
        await userEvent.click(screen.getByRole("button", { name: "Execute Mint" }));

        await waitFor(() => {
            expect(screen.getByText("Successfully minted 150 $RPC!")).toBeInTheDocument();
        });
        
        // Verify the correct payload was POSTed
        expect(fetchMock).toHaveBeenCalledWith("http://localhost:3001/wallets/fund", expect.objectContaining({
            method: "POST",
            body: JSON.stringify({ amount: 150 })
        }));
    });

    // 9. Dev Sandbox Failure
    it("shows error when dev minting fails", async () => {
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({ status: { claimed: true } }),
            })
            .mockResolvedValueOnce({
                ok: false,
                json: () => Promise.resolve({ error: "Unauthorized mint attempt" }),
            });
        
        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);

        await waitFor(() => expect(screen.getByPlaceholderText("Amount (e.g. 500)")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("Amount (e.g. 500)"), "100");
        await userEvent.click(screen.getByRole("button", { name: "Execute Mint" }));

        await waitFor(() => {
            expect(screen.getByText("Mint Error: Unauthorized mint attempt")).toBeInTheDocument();
        });
    });
});