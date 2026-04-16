import { MemoryRouter } from "react-router-dom";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ListingModal from "../Components/ListingModal.jsx";

const mockNavigate = vi.fn();

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe("ListingModal", () => {
  const mockItemData = {
    item: { 
      itemId: 12, 
      name: "Mechanical Keyboard", 
      description: "Hot swappable", 
      price: 89.5, 
      stock: 7 
    },
    images: [{ imageUrl: "/test-image.jpg" }],
    sellerName: "Dana"
  };

  beforeEach(() => {
    vi.resetAllMocks();
    vi.stubGlobal("fetch", vi.fn());
    
    // Default mock to resolve the initial fetch on mount
    fetch.mockResolvedValue({
      ok: true,
      json: async () => mockItemData,
    });
  });

  it("navigates to the order page from buy it now with the selected quantity", async () => {
    render(
      <MemoryRouter>
        <ListingModal itemId={12} onClose={vi.fn()} />
      </MemoryRouter>
    );

    const quantityInput = await screen.findByRole("spinbutton", { name: /quantity/i });
    await userEvent.clear(quantityInput);
    await userEvent.type(quantityInput, "3");
    await userEvent.click(screen.getByRole("button", { name: /buy it now/i }));

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
    // 1. Mock GET cart, then POST to cart
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => mockItemData }) // Initial mount fetch
      .mockResolvedValueOnce({ ok: true, json: async () => ({ cart: [] }) }) // GET /cart
      .mockResolvedValueOnce({ ok: true, json: async () => ({ success: true }) }); // POST /cart

    render(
      <MemoryRouter>
        <ListingModal itemId={12} onClose={vi.fn()} />
      </MemoryRouter>
    );

    const quantityInput = await screen.findByRole("spinbutton", { name: /quantity/i });
    await userEvent.clear(quantityInput);
    await userEvent.type(quantityInput, "4");
    await userEvent.click(screen.getByRole("button", { name: /add to cart/i }));

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:3001/cart",
      expect.objectContaining({ method: "GET" })
    );
    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:3001/cart/12?quantity=4",
      expect.objectContaining({ method: "POST" })
    );
    
    // Your code renders: `${quantity} added to your cart.`
    expect(await screen.findByText(/4 added to your cart/i)).toBeInTheDocument();
  });

  it("shows the stock limit message when adding more would exceed stock", async () => {
    // 1. Mock fetch for data, then mock GET cart showing 3 already inside
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => ({ ...mockItemData, item: { ...mockItemData.item, stock: 4 } }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ cart: [{ itemId: 12, quantity: 3 }] }) });

    render(
      <MemoryRouter>
        <ListingModal itemId={12} onClose={vi.fn()} />
      </MemoryRouter>
    );

    const quantityInput = await screen.findByRole("spinbutton", { name: /quantity/i });
    await userEvent.clear(quantityInput);
    await userEvent.type(quantityInput, "2"); // 3 + 2 = 5 (exceeds stock of 4)
    await userEvent.click(screen.getByRole("button", { name: /add to cart/i }));

    expect(await screen.findByText(/cart is over the limit/i)).toBeInTheDocument();
  });

  it("disables purchase actions for sold out items", async () => {
    // Mock item with 0 stock
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        ...mockItemData,
        item: { ...mockItemData.item, stock: 0 }
      }),
    });

    render(
      <MemoryRouter>
        <ListingModal itemId={12} onClose={vi.fn()} />
      </MemoryRouter>
    );

    // In your code: isSoldOut ? "Sold Out" : "Buy It Now"
    const buyItNowBtn = await screen.findByRole("button", { name: /sold out/i });
    const addToCartBtn = screen.getByRole("button", { name: /add to cart/i });

    expect(buyItNowBtn).toBeDisabled();
    expect(addToCartBtn).toBeDisabled();

    // In your code: {!isSoldOut && <label>...Quantity...</label>}
    // This means the input should NOT be in the document
    expect(screen.queryByRole("spinbutton", { name: /quantity/i })).not.toBeInTheDocument();
  });
});