import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import Navbar from "../Components/Navbar.jsx";
import { use } from "react";
import { useAuth } from "../Auth/AuthContext.jsx";

vi.mock("../Auth/AuthContext.jsx", () => ({
    useAuth: () => ({
        user: null,
        signOut: vi.fn(),
    }),
}));

describe("Navbar", () => {
    // Tests the resulting action when the home logo is clicked. Expects redirection to the home page
    it("calls onNavigate with 'home' when logo is clicked", async () => {
        const onNavigate = vi.fn();
        const onSignInClick = vi.fn();
        render(<Navbar onSignInClick={onSignInClick} onNavigate={onNavigate} currentPage="listings" />);

        await userEvent.click(screen.getByText("$RPC Market"));
        expect(onNavigate).toHaveBeenCalledWith("home");
    });

    // Tests the resulting action when the marketplace card is clicked on the landing page. Expects redirection to the listings page
    it("calls onNavigate with 'listings' when Marketplace card is clicked", async () => {
        const onNavigate = vi.fn();
        const onSignInClick = vi.fn();
        render(<Navbar onSignInClick={onSignInClick} onNavigate={onNavigate} currentPage="home" />);
        
        await userEvent.click(screen.getByRole("button", {name: "Marketplace"}));
        expect(onNavigate).toHaveBeenCalledWith("listings");
    });

    // Tests the resulting action when the sell card is clicked on the landing page. Expects redirection to the sell page
    it("calls onNavigate with 'sell' when Sell card is clicked", async () => {
        const onNavigate = vi.fn();
        render(<Navbar onNavigate={onNavigate} />);

        await userEvent.click(screen.getByText("Sell"));
        expect(onNavigate).toHaveBeenCalledWith("sell");
    });

    // Tests the resulting action when the sign in button is clicked. Expects the onSignInClick function to be called, which should open the sign in modal
    it("calls onSignInClick when Sign In button is clicked", async () => {
        const onSignInClick = vi.fn();
        render(<Navbar onSignInClick={onSignInClick} onNavigate={() => {}} currentPage="home" />);

        await userEvent.click(screen.getByRole("button", {name: "Sign In"}));
        expect(onSignInClick).toHaveBeenCalled();
    });
});