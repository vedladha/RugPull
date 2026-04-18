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
      userBalance: 250,
      updateUserBalance: vi.fn().mockResolvedValue(250),
    });
  });

  it("renders a sign-in prompt when the user is not authenticated", () => {
    mockUseAuth.mockReturnValue({
      user: null,
      userBalance: null,
      updateUserBalance: vi.fn().mockResolvedValue(null),
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

    expect(screen.getByText("Sign in to complete your order.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Go to Sign In" })).toBeInTheDocument();
  });

  it("renders the empty checkout state when opened without order data", () => {
    mockUseAuth.mockReturnValue({
      user: { userId: 1, displayName: "buyer" },
      userBalance: 250,
      updateUserBalance: vi.fn().mockResolvedValue(250),
    });

    renderOrderPage(undefined);

    expect(screen.getByText("No items are ready for checkout.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Browse Marketplace" })).toBeInTheDocument();
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

  it("clamps quantity controls to the available stock and blocks sold out checkout", async () => {
    renderOrderPage({
      source: "cart",
      items: [
        {
          itemId: 1,
          name: "Camera",
          description: "4k body",
          price: 100,
          sellerName: "Bob",
          stock: 2,
          quantity: 1,
          fromCart: true,
        },
        {
          itemId: 2,
          name: "Tripod",
          description: "Carbon fiber",
          price: 25,
          sellerName: "Carol",
          stock: 0,
          quantity: 1,
          fromCart: true,
        },
      ],
    });

    const decreaseButton = screen.getByRole("button", { name: "Decrease quantity for Camera" });
    const increaseButton = screen.getByRole("button", { name: "Increase quantity for Camera" });
    const submitButton = screen.getByRole("button", { name: "Place orders" });

    expect(decreaseButton).toBeDisabled();
    await userEvent.click(increaseButton);
    expect(increaseButton).toBeDisabled();
    expect(screen.getByText("Sold out")).toBeInTheDocument();
    expect(screen.getByText("Remove sold out items before placing this order.")).toBeInTheDocument();
    expect(submitButton).toBeDisabled();
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

  it("shows a removal error and keeps the item when cart deletion fails", async () => {
    mockFetch.mockResolvedValueOnce({ ok: false });

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

    expect(await screen.findByText("Failed to remove item")).toBeInTheDocument();
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

  it("shows an unavailable wallet state when the balance request fails", async () => {
    mockUseAuth.mockReturnValue({
      user: { userId: 1, displayName: "buyer" },
      userBalance: null,
      updateUserBalance: vi.fn().mockResolvedValue(null),
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

    expect(await screen.findByText("Unavailable")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Place order" })).toBeDisabled();
  });

  it("blocks checkout when the known balance is below the order total", async () => {
    mockUseAuth.mockReturnValue({
      user: { userId: 1, displayName: "buyer" },
      userBalance: 10,
      updateUserBalance: vi.fn().mockResolvedValue(10),
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

    expect(await screen.findByText("Insufficient RPC balance for this order.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Place order" })).toBeDisabled();
  });
});
