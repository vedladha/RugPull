import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import App from "../App.jsx";
import { useAuth } from "../Auth/auth-context.js";

vi.mock("../Auth/auth-context");

beforeEach(() => {
    vi.mocked(useAuth).mockReturnValue({
        user: null,
        userBalance: -99.99,
        updateUserBalance: vi.fn(),
        signOut: vi.fn(),
        walletBalance: vi.fn().mockResolvedValue(-999.99),
        profileDetails: vi.fn().mockResolvedValue({ profile: {} }),
        updateProfile: vi.fn(),
        getWishlistItems: vi.fn().mockResolvedValue([]),
        removeFromWishlist: vi.fn(),
    });

    // Stub window.scrollTo since JSDOM doesn't implement a real window
    window.scrollTo = vi.fn();

    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ items: [] }),
    }));
});

// App needs to be wrapped in MemoryRouter since it uses useNavigate
const renderApp = (initialPath = "/") => {
    return render(
        <MemoryRouter initialEntries={[initialPath]}>
            <App />
        </MemoryRouter>
    );
};

describe("App", () => {
    it("renders home page by default", () => {
        renderApp();
        // UPDATED: Now checks for the text in the new FrontPage component
        expect(screen.getByText("What would you like to do?")).toBeInTheDocument();
    });

    it("renders navbar on all pages", () => {
        renderApp();
        expect(screen.getAllByText("$RPC Market").length).toBeGreaterThan(0);
    });

    it("renders footer on all pages", async () => {
        renderApp();
        await waitFor(() => {
            expect(screen.getByRole("contentinfo")).toBeInTheDocument();
        });
    });

    it("navigates to login page when Sign In is clicked", async () => {
        renderApp();
        await userEvent.click(screen.getByRole("button", { name: "Sign In" }));

        // AuthPage renders an h1 with "Welcome Back" for the login route
        expect(await screen.findByRole("heading", { name: "Welcome Back" })).toBeInTheDocument();
    });

    it("navigates to signup page when Create Account is clicked", async () => {
        renderApp();
        await userEvent.click(screen.getByText("Create Account"));

        // AuthPage renders an h1 with "Create Account" for the signup route
        expect(await screen.findByRole("heading", { name: "Create Account" })).toBeInTheDocument();
    });

    it("renders listings page at /listings", async () => {
        renderApp("/listings");
        await waitFor(() => {
            expect(screen.getByText("Active Listings")).toBeInTheDocument();
        });
    });

    it("renders sell page at /sell", () => {
        renderApp("/sell");
        // THE FIX: Looking for the new text rendered by the SignInPrompt component
        expect(screen.getByText(/Please sign in to manage your inventory/i)).toBeInTheDocument();
    });

    it("renders profile page at /profile", async () => {
        // Override the default mock specifically for the profile test
        vi.mocked(useAuth).mockReturnValue({
            user: { email: "test@example.com" },
            userBalance: 100,
            profileDetails: vi.fn().mockResolvedValue({
                profile: { displayName: "TestUser", bio: "Bio" }
            }),
            updateProfile: vi.fn(),
            signOut: vi.fn(),
            walletBalance: vi.fn().mockResolvedValue(-999.99),
            getWishlistItems: vi.fn().mockResolvedValue([]),
            removeFromWishlist: vi.fn(),
        });

        renderApp("/profile");
        await waitFor(() => {
            expect(screen.getByText("Your Profile")).toBeInTheDocument();
        });
    });

    it("renders wishlist page at /wishlist", async () => {
        vi.mocked(useAuth).mockReturnValue({
            user: { email: "test@example.com", displayName: "Test User" },
            signOut: vi.fn(),
            walletBalance: vi.fn().mockResolvedValue(-999.99),
            profileDetails: vi.fn().mockResolvedValue({ profile: {} }),
            updateProfile: vi.fn(),
            getWishlistItems: vi.fn().mockResolvedValue([]),
            removeFromWishlist: vi.fn(),
        });

        renderApp("/wishlist");

        await waitFor(() => {
            expect(screen.getByText("Your Wishlist")).toBeInTheDocument();
        });
    });
});