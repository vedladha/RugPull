import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import App from "../App.jsx";

const mockUseAuth = vi.fn();

vi.mock("../Auth/auth-context", () => ({
    useAuth: () => mockUseAuth(),
}));

beforeEach(() => {
    mockUseAuth.mockReturnValue({
        user: null,
        signOut: vi.fn(),
        walletBalance: vi.fn().mockResolvedValue(-999.99),
        getWishlistItems: vi.fn().mockResolvedValue([]),
        removeFromWishlist: vi.fn(),
    });
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
    // Tests that the home page renders by default
    it("renders home page by default", () => {
        renderApp();
        expect(screen.getByText("Where do you want to go?")).toBeInTheDocument();
    });

    // Tests that the navbar is always rendered
    it("renders navbar on all pages", () => {
        renderApp();
        expect(screen.getAllByText("$RPC Market").length).toBeGreaterThan(0);
    });

    // Tests that the footer is always rendered
    it("renders footer on all pages", async () => {
        renderApp();
        await waitFor(() => {
            expect(screen.getByRole("contentinfo")).toBeInTheDocument();
        });
    });

    // Tests that sign in modal opens when Sign In is clicked
    it("opens sign in modal when Sign In is clicked", async () => {
        renderApp();
        await userEvent.click(screen.getByRole("button", { name: "Sign In" }));
        expect(screen.getByText("Welcome back")).toBeInTheDocument();
    });

    // Tests that sign up modal opens when Create Account is clicked on hero
    it("opens sign up modal when Create Account is clicked", async () => {
        renderApp();
        await userEvent.click(screen.getByText("Create Account"));
        expect(screen.getByText("Create account")).toBeInTheDocument();
    });

    // Tests that modal closes when close button is clicked
    it("closes modal when close button is clicked", async () => {
        renderApp();
        await userEvent.click(screen.getByRole("button", { name: "Sign In" }));
        expect(screen.getByText("Welcome back")).toBeInTheDocument();

        await userEvent.click(screen.getByText("✕"));
        expect(screen.queryByText("Welcome back")).toBeNull();
    });

    // Tests that navigating to /listings renders the listings page
    it("renders listings page at /listings", async () => {
        renderApp("/listings");
        await waitFor(() => {
            expect(screen.getByText("Active Listings")).toBeInTheDocument();
        });
    });

    // Tests that navigating to /sell renders the sell page
    it("renders sell page at /sell", () => {
        renderApp("/sell");
        expect(screen.getByText("List an Item")).toBeInTheDocument();
    });

    // Tests that navigating to /profile renders the profile page
    it("renders profile page at /profile", async () => {
        mockUseAuth.mockReturnValue({
            user: { email: "test@example.com" },
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
        mockUseAuth.mockReturnValue({
            user: { email: "test@example.com", displayName: "Test User" },
            signOut: vi.fn(),
            walletBalance: vi.fn().mockResolvedValue(-999.99),
            getWishlistItems: vi.fn().mockResolvedValue([]),
            removeFromWishlist: vi.fn(),
        });

        renderApp("/wishlist");

        await waitFor(() => {
            expect(screen.getByText("Your Wishlist")).toBeInTheDocument();
        });
    });
});
