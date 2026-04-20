import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import History from "../History.jsx"; // Adjust path if placed in a tests folder

// Mock the auth context
const mockUseAuth = vi.fn();
vi.mock("../Auth/auth-context", () => ({
  useAuth: () => mockUseAuth(),
}));

const renderHistory = () => {
  return render(
    <MemoryRouter>
      <History />
    </MemoryRouter>
  );
};

describe("History Component", () => {
  const mockOrders = [
    {
      id: 1,
      orderType: "buy",
      itemName: "Iron Sword",
      totalPrice: 50,
      quantity: 1,
      sellerName: "MerchantA",
      createdAt: "2023-10-01T10:00:00Z", // Older date
    },
    {
      id: 2,
      orderType: "sell",
      itemName: "Health Potion",
      totalPrice: 150,
      quantity: 3,
      buyerName: "HeroB",
      createdAt: "2023-10-05T10:00:00Z", // Newer date
    },
  ];

  beforeEach(() => {
    vi.resetAllMocks();
    vi.stubGlobal("fetch", vi.fn());

    // Default to an authenticated user
    mockUseAuth.mockReturnValue({
      user: { id: 1, email: "test@example.com" },
    });
  });

  it("renders the sign-in prompt when the user is not authenticated", () => {
    mockUseAuth.mockReturnValue({ user: null });
    renderHistory();

    expect(screen.getByRole("heading", { name: "History" })).toBeInTheDocument();
    expect(screen.getByText(/Please sign in to view your transaction history/i)).toBeInTheDocument();
  });

  it("shows a loading state initially", async () => {
    // Create a fetch mock that doesn't resolve immediately
    let resolveFetch;
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise((resolve) => { resolveFetch = resolve; }))
    );

    renderHistory();
    expect(screen.getByText("Loading transactions…")).toBeInTheDocument();

    // Resolve the promise to clean up the test
    resolveFetch({ ok: true, json: () => Promise.resolve({ orders: [] }) });
  });

  it("displays an error if the API request fails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
    }));

    renderHistory();

    await waitFor(() => {
      expect(screen.getByText("Failed to load transaction history.")).toBeInTheDocument();
    });
  });

  it("renders orders and calculates statistics correctly", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ orders: mockOrders }),
    }));

    renderHistory();

    await waitFor(() => {
      expect(screen.queryByText("Loading transactions…")).not.toBeInTheDocument();
    });

    // Check Stats Calculation
    // Earned: 150, Spent: 50, Net: +100
    expect(screen.getByText("$150.00")).toBeInTheDocument(); // Earned
    expect(screen.getByText("$50.00")).toBeInTheDocument(); // Spent
    expect(screen.getByText("+$100.00")).toBeInTheDocument(); // Net
    expect(screen.getByText("2")).toBeInTheDocument(); // Transactions count

    // Check Items rendering
    expect(screen.getByText("Iron Sword")).toBeInTheDocument();
    expect(screen.getByText("From MerchantA")).toBeInTheDocument();
    
    expect(screen.getByText("Health Potion")).toBeInTheDocument();
    expect(screen.getByText("To HeroB")).toBeInTheDocument();
  });

  it("filters transactions when clicking 'Purchases' or 'Sales'", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ orders: mockOrders }),
    }));

    renderHistory();
    await waitFor(() => expect(screen.getByText("Iron Sword")).toBeInTheDocument());

    // Click Purchases Filter
    const purchasesBtn = screen.getByRole("button", { name: "Purchases" });
    await userEvent.click(purchasesBtn);

    expect(screen.getByText("Iron Sword")).toBeInTheDocument();
    expect(screen.queryByText("Health Potion")).not.toBeInTheDocument();

    // Click Sales Filter
    const salesBtn = screen.getByRole("button", { name: "Sales" });
    await userEvent.click(salesBtn);

    expect(screen.queryByText("Iron Sword")).not.toBeInTheDocument();
    expect(screen.getByText("Health Potion")).toBeInTheDocument();

    // Click All Filter
    const allBtn = screen.getByRole("button", { name: "All" });
    await userEvent.click(allBtn);

    expect(screen.getByText("Iron Sword")).toBeInTheDocument();
    expect(screen.getByText("Health Potion")).toBeInTheDocument();
  });

  it("sorts transactions based on the selected dropdown option", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ orders: mockOrders }),
    }));

    renderHistory();
    await waitFor(() => expect(screen.getByText("Iron Sword")).toBeInTheDocument());

    const sortSelect = screen.getByRole("combobox");

    // Helper to get titles in current rendered order
    const getRenderedOrder = () => 
      screen.getAllByText(/Iron Sword|Health Potion/).map(el => el.textContent);

    // Default sort is date-desc (Newest first)
    // Health Potion (Oct 5) should be before Iron Sword (Oct 1)
    expect(getRenderedOrder()).toEqual(["Health Potion", "Iron Sword"]);

    // Change to date-asc (Oldest first)
    await userEvent.selectOptions(sortSelect, "date-asc");
    expect(getRenderedOrder()).toEqual(["Iron Sword", "Health Potion"]);

    // Change to amount-asc (Low -> High)
    await userEvent.selectOptions(sortSelect, "amount-asc");
    // 50 < 150
    expect(getRenderedOrder()).toEqual(["Iron Sword", "Health Potion"]);

    // Change to amount-desc (High -> Low)
    await userEvent.selectOptions(sortSelect, "amount-desc");
    // 150 > 50
    expect(getRenderedOrder()).toEqual(["Health Potion", "Iron Sword"]);
  });

  it("displays a fallback message when there are no transactions", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ orders: [] }),
    }));

    renderHistory();

    await waitFor(() => {
      expect(screen.getByText("No transactions found.")).toBeInTheDocument();
    });
  });
});