import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import PageCards from "../Components/Navbar.jsx";
import { use } from "react";
import { useAuth } from "../Auth/AuthContext.jsx";

vi.mock("../Auth/AuthContext.jsx", () => ({
    useAuth: () => ({
        user: null,
        signOut: vi.fn(),
    }),
}));

describe("Navbar", () => {
    // Tests the resulting action when the marketplace card is clicked on the landing page. Expects redirection to the listings page
    it("calls onNavigate with 'listings' when Marketplace card is clicked", async () => {
        const onNavigate = vi.fn();
        const onCardClick = vi.fn();
        render(<PageCards onCardClick={onCardClick} onNavigate={onNavigate}/>);
        
        await userEvent.click(screen.getByRole("button", {name: "Marketplace"}));
        expect(onNavigate).toHaveBeenCalledWith("listings");
    });

    // Tests the resulting action when the sell card is clicked on the landing page. Expects redirection to the sell page
    it("calls onNavigate with 'sell' when Sell card is clicked", async () => {
        const onNavigate = vi.fn();
        render(<PageCards onNavigate={onNavigate} />);

        await userEvent.click(screen.getByText("Sell"));
        expect(onNavigate).toHaveBeenCalledWith("sell");
    });
});