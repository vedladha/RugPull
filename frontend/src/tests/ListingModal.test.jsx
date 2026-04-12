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
    vi.stubGlobal("fetch", vi.fn());
  });

  it("navigates to the order page from buy it now with the selected quantity", async () => {
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

    const quantityInput = screen.getByRole("spinbutton", { name: "Quantity" });
    await userEvent.clear(quantityInput);
    await userEvent.type(quantityInput, "3");
    await userEvent.click(screen.getByRole("button", { name: "Buy It Now" }));

    expect(mockNavigate).toHaveBeenCalledWith("/order", {
      state: {
        source: "listing",
        items: [
          expect.objectContaining({
            itemId: 12,
            quantity: 3,
            fromCart: false,
          }),
        ],
      },
    });
  });

  it("adds the selected quantity to the cart", async () => {
    fetch
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ cart: [] }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ cart: { itemId: 12, quantity: 4 } }),
      });

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

    const quantityInput = screen.getByRole("spinbutton", { name: "Quantity" });
    await userEvent.clear(quantityInput);
    await userEvent.type(quantityInput, "4");
    await userEvent.click(screen.getByRole("button", { name: "Add to Cart" }));

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:3001/cart",
      expect.objectContaining({
        method: "GET",
        credentials: "include",
      }),
    );
    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:3001/cart/12?quantity=4",
      expect.objectContaining({
        method: "POST",
        credentials: "include",
      }),
    );
    expect(await screen.findByText("4 items added to your cart.")).toBeInTheDocument();
  });

  it("shows the stock limit message when adding more would exceed stock", async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ cart: [{ itemId: 12, quantity: 3 }] }),
    });

    render(
      <ListingModal
        listing={{
          itemId: 12,
          name: "Mechanical Keyboard",
          description: "Hot swappable",
          price: 89.5,
          sellerName: "Dana",
          stock: 4,
        }}
        onClose={vi.fn()}
      />,
    );

    const quantityInput = screen.getByRole("spinbutton", { name: "Quantity" });
    await userEvent.clear(quantityInput);
    await userEvent.type(quantityInput, "2");
    await userEvent.click(screen.getByRole("button", { name: "Add to Cart" }));

    expect(await screen.findByText("Cart is over the limit for this item")).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it("disables purchase actions for sold out items", () => {
    render(
      <ListingModal
        listing={{
          itemId: 44,
          name: "Mechanical Keyboard",
          description: "Hot swappable",
          price: 89.5,
          sellerName: "Dana",
          stock: 0,
        }}
        onClose={vi.fn()}
      />,
    );

    expect(screen.getAllByRole("button", { name: "Sold Out" })).toHaveLength(2);
    expect(screen.getByRole("spinbutton", { name: "Quantity" })).toBeDisabled();
  });
});
