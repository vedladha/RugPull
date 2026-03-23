import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import PageCards from "../Components/PageCards.jsx";

describe("PageCards", () => {
    it("calls onCardClick with 'marketplace' when Marketplace card is clicked", async () => {
        const onCardClick = vi.fn();
        render(<PageCards onCardClick={onCardClick} />);

        await userEvent.click(screen.getByText("Marketplace"));
        expect(onCardClick).toHaveBeenCalledWith("marketplace");
    });

    it("calls onCardClick with 'sell' when Sell card is clicked", async () => {
        const onCardClick = vi.fn();
        render(<PageCards onCardClick={onCardClick} />);

        await userEvent.click(screen.getByText("Sell"));
        expect(onCardClick).toHaveBeenCalledWith("sell");
    });

    it("does not throw when onCardClick is not provided", async () => {
        render(<PageCards />);
        await userEvent.click(screen.getByText("Marketplace"));
        // no error should be thrown
    });
});