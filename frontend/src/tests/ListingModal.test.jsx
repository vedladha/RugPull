import { MemoryRouter } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ListingModal from "../Components/ListingModal.jsx";

const mockNavigate = vi.fn();
const mockUseAuth = vi.fn();

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("../Auth/auth-context", () => ({
  useAuth: () => mockUseAuth(),
}));

describe("ListingModal", () => {
  // Updated mock to match the new flat data structure expected by the component
  const mockListing = {
    itemId: 12,
    name: "Mechanical Keyboard",
    description: "Hot swappable",
    price: 89.5,
    stock: 7,
    thumbnailUrl: "/test-image.jpg",
    sellerName: "Dana"
  };

  beforeEach(() => {
    vi.resetAllMocks();
    vi.stubGlobal("fetch", vi.fn());
    mockUseAuth.mockReturnValue({
      user: null,
      getUserRating: vi.fn(),
      createRating: vi.fn(),
      updateRating: vi.fn(),
      deleteRating: vi.fn(),
    });
  });

  it("navigates to the order page from buy it now with the selected quantity", async () => {
    render(
      <MemoryRouter>
        <ListingModal listing={mockListing} onClose={vi.fn()} />
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
    // Mock GET cart (empty), then POST to cart (success)
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => ({ cart: [] }) }) 
      .mockResolvedValueOnce({ ok: true, json: async () => ({ success: true }) }); 

    render(
      <MemoryRouter>
        <ListingModal listing={mockListing} onClose={vi.fn()} />
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
    
    expect(await screen.findByText(/4 items added to your cart/i)).toBeInTheDocument();
  });

  it("shows the stock limit message when adding more would exceed stock", async () => {
    // Mock GET cart showing 3 already inside
    fetch.mockResolvedValueOnce({ ok: true, json: async () => ({ cart: [{ itemId: 12, quantity: 3 }] }) });

    // Artificially lower the stock for this test
    const limitedListing = { ...mockListing, stock: 4 };

    render(
      <MemoryRouter>
        <ListingModal listing={limitedListing} onClose={vi.fn()} />
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
    const soldOutListing = { ...mockListing, stock: 0 };

    render(
      <MemoryRouter>
        <ListingModal listing={soldOutListing} onClose={vi.fn()} />
      </MemoryRouter>
    );

    // Both "Buy It Now" and "Add to Cart" change to "Sold Out"
    const soldOutBtns = await screen.findAllByRole("button", { name: /sold out/i });
    
    expect(soldOutBtns).toHaveLength(2);
    expect(soldOutBtns[0]).toBeDisabled();
    expect(soldOutBtns[1]).toBeDisabled();

    // THE FIX: Expect the input to be in the document, but disabled
    const quantityInput = screen.getByRole("spinbutton", { name: /quantity/i });
    expect(quantityInput).toBeInTheDocument();
    expect(quantityInput).toBeDisabled();
  });
});