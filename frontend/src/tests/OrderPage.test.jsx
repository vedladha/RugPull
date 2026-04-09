import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import OrderPage from "../Pages/OrderPage.jsx";

const mockUseAuth = vi.fn();
const mockFetch = vi.fn();

vi.mock("../Auth/auth-context.js", () => ({
  useAuth: () => mockUseAuth(),
}));

function renderOrderPage(initialState) {
  return render(
    <MemoryRouter initialEntries={[{ pathname: "/order", state: initialState }]}>
      <Routes>
        <Route path="/order" element={<OrderPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("OrderPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.stubGlobal("fetch", mockFetch);

    mockUseAuth.mockReturnValue({
      user: { userId: 1, displayName: "buyer" },
      walletBalance: vi.fn().mockResolvedValue(250),
    });
  });

  it("places a buy-it-now order from route state", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        order: { orderId: 77 },
      }),
    });

    renderOrderPage({
      source: "listing",
      items: [
        {
          itemId: 9,
          name: "Ledger Wallet",
          description: "Hardware wallet",
          price: 49.99,
          sellerName: "Alice",
          stock: 3,
          quantity: 1,
        },
      ],
    });

    expect(screen.getByText("Ledger Wallet")).toBeInTheDocument();
    expect(screen.getByText("Confirm your order.")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Place order" }));

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:3001/orders",
        expect.objectContaining({
          method: "POST",
          credentials: "include",
          body: JSON.stringify({
            items: [{ itemId: 9, quantity: 1 }],
          }),
        }),
      );
    });

    expect(await screen.findByText("Order submitted.")).toBeInTheDocument();
  });

  it("submits cart checkout as one batched order and clears cart items after success", async () => {
    mockFetch
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          order: { orderId: 11 },
        }),
      })
      .mockResolvedValueOnce({ ok: true })
      .mockResolvedValueOnce({ ok: true });

    renderOrderPage({
      source: "cart",
      items: [
        {
          itemId: 1,
          name: "Camera",
          description: "4k body",
          price: 100,
          sellerName: "Bob",
          stock: 4,
          quantity: 2,
          fromCart: true,
        },
        {
          itemId: 2,
          name: "Tripod",
          description: "Carbon fiber",
          price: 25,
          sellerName: "Carol",
          stock: 1,
          quantity: 1,
          fromCart: true,
        },
      ],
    });

    await userEvent.click(screen.getByRole("button", { name: "Place orders" }));

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:3001/orders",
        expect.objectContaining({
          method: "POST",
          credentials: "include",
          body: JSON.stringify({
            items: [
              { itemId: 1, quantity: 2 },
              { itemId: 2, quantity: 1 },
            ],
          }),
        }),
      );
    });

    expect(mockFetch).toHaveBeenCalledWith(
      "http://localhost:3001/cart/1",
      expect.objectContaining({
        method: "DELETE",
        credentials: "include",
      }),
    );
    expect(mockFetch).toHaveBeenCalledWith(
      "http://localhost:3001/cart/2",
      expect.objectContaining({
        method: "DELETE",
        credentials: "include",
      }),
    );
    expect(await screen.findByText("Order submitted.")).toBeInTheDocument();
  });

  it("shows backend batch errors without clearing the checkout items", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      json: async () => ({
        error: "Insufficient stock",
      }),
    });

    renderOrderPage({
      source: "cart",
      items: [
        {
          itemId: 2,
          name: "Tripod",
          description: "Carbon fiber",
          price: 25,
          sellerName: "Carol",
          stock: 1,
          quantity: 1,
          fromCart: true,
        },
      ],
    });

    await userEvent.click(screen.getByRole("button", { name: "Place order" }));

    expect(await screen.findByText("Insufficient stock")).toBeInTheDocument();
    expect(screen.getByText("Tripod")).toBeInTheDocument();
  });

  it("removes a cart item from checkout and from the cart backend", async () => {
    mockFetch.mockResolvedValueOnce({ ok: true });

    renderOrderPage({
      source: "cart",
      items: [
        {
          itemId: 2,
          name: "Tripod",
          description: "Carbon fiber",
          price: 25,
          sellerName: "Carol",
          stock: 1,
          quantity: 1,
          fromCart: true,
        },
      ],
    });

    await userEvent.click(screen.getByRole("button", { name: "Remove Tripod from order" }));

    expect(mockFetch).toHaveBeenCalledWith(
      "http://localhost:3001/cart/2",
      expect.objectContaining({
        method: "DELETE",
        credentials: "include",
      }),
    );
    expect(await screen.findByText("No items are ready for checkout.")).toBeInTheDocument();
  });

  it("removes a buy-now item locally without calling the cart api", async () => {
    renderOrderPage({
      source: "listing",
      items: [
        {
          itemId: 9,
          name: "Ledger Wallet",
          description: "Hardware wallet",
          price: 49.99,
          sellerName: "Alice",
          stock: 3,
          quantity: 1,
          fromCart: false,
        },
      ],
    });

    await userEvent.click(screen.getByRole("button", { name: "Remove Ledger Wallet from order" }));

    expect(mockFetch).not.toHaveBeenCalled();
    expect(await screen.findByText("No items are ready for checkout.")).toBeInTheDocument();
  });
});
