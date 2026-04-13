import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ListingModal from "../Components/ListingModal.jsx";

const mockNavigate = vi.fn();

vi.mock("react-router-dom", () => ({
  useNavigate: () => mockNavigate,
}));

describe("ListingModal", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("navigates to the order page from buy it now", async () => {
    render(
      <ListingModal
        listing={{
          itemId: 12,
          name: "Mechanical Keyboard",
          description: "Hot swappable",
          price: 89.5,
          sellerName: "Dana",
          stock: 7,
        }}
        onClose={vi.fn()}
      />,
    );

    await userEvent.click(screen.getByRole("button", { name: "Buy It Now" }));

    expect(mockNavigate).toHaveBeenCalledWith("/order", {
      state: {
        source: "listing",
        items: [
          expect.objectContaining({
            itemId: 12,
            quantity: 1,
            fromCart: false,
          }),
        ],
      },
    });
  });
});
