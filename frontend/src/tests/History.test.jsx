import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import History from "../History";
 
// ── Mock data ──────────────────────────────────────────────────────────────
const mockHistory = [
  { id: 1, type: "buy",  itemName: "Nike Air Force 1",    amount: 120.00, date: "2024-03-15", quantity: 1 },
  { id: 2, type: "sell", itemName: "Jordan 1 Retro High", amount: 350.00, date: "2024-03-10", quantity: 1 },
  { id: 3, type: "buy",  itemName: "New Balance 550",     amount: 90.00,  date: "2024-03-08", quantity: 2 },
  { id: 4, type: "sell", itemName: "Yeezy Boost 350",     amount: 280.00, date: "2024-02-28", quantity: 1 },
];
 
// ── Setup ──────────────────────────────────────────────────────────────────
beforeEach(() => {
  // Provide a JWT token so the fetch guard doesn't bail early
  localStorage.setItem("token", "mock-jwt-token");
 
  // Stub global fetch to return mock data
  global.fetch = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => mockHistory,
  });
});
 
// ── Tests ──────────────────────────────────────────────────────────────────
describe("History page", () => {
 
  // ── Rendering ────────────────────────────────────────────────────────────
  describe("renders page structure", () => {
    it("shows the page heading", async () => {
      render(<History />);
      expect(await screen.findByText("Transaction History")).toBeInTheDocument();
    });
 
    it("shows the Account tag", async () => {
      render(<History />);
      expect(await screen.findByText("Account")).toBeInTheDocument();
    });
 
    it("shows the subtitle", async () => {
      render(<History />);
      expect(await screen.findByText(/complete record of purchases and sales/i)).toBeInTheDocument();
    });
  });
 
  // ── Stats ────────────────────────────────────────────────────────────────
  describe("summary stats", () => {
    it("renders all four stat labels", async () => {
      render(<History />);
      expect(await screen.findByText(/total earned/i)).toBeInTheDocument();
      expect(screen.getByText(/total spent/i)).toBeInTheDocument();
      expect(screen.getByText(/^net$/i)).toBeInTheDocument();
      expect(screen.getByText(/transactions/i)).toBeInTheDocument();
    });
 
    it("calculates total earned from sell transactions", async () => {
      render(<History />);
      // sells: 350 + 280 = 630
      expect(await screen.findByText("$630.00")).toBeInTheDocument();
    });
 
    it("calculates total spent from buy transactions", async () => {
      render(<History />);
      // buys: 120 + 90 = 210
      expect(await screen.findByText("$210.00")).toBeInTheDocument();
    });
 
    it("shows correct transaction count", async () => {
      render(<History />);
      await screen.findByText("Transaction History");
      // 4 total transactions
      const countEl = screen.getByText("4");
      expect(countEl).toBeInTheDocument();
    });
  });
 
  // ── Transaction rows ─────────────────────────────────────────────────────
  describe("transaction list", () => {
    it("renders all transactions by default", async () => {
      render(<History />);
      expect(await screen.findByText("Nike Air Force 1")).toBeInTheDocument();
      expect(screen.getByText("Jordan 1 Retro High")).toBeInTheDocument();
      expect(screen.getByText("New Balance 550")).toBeInTheDocument();
      expect(screen.getByText("Yeezy Boost 350")).toBeInTheDocument();
    });
 
    it("renders buy type chips", async () => {
      render(<History />);
      await screen.findByText("Nike Air Force 1");
      const chips = screen.getAllByText(/^purchase$/i);
      expect(chips).toHaveLength(2); // 2 buy items
    });
 
    it("renders sell type chips", async () => {
      render(<History />);
      await screen.findByText("Jordan 1 Retro High");
      const chips = screen.getAllByText(/^sale$/i);
      expect(chips).toHaveLength(2); // 2 sell items
    });
 
    it("renders formatted amounts with correct sign", async () => {
      render(<History />);
      await screen.findByText("Nike Air Force 1");
      expect(screen.getByText("−$120.00")).toBeInTheDocument(); // buy
      expect(screen.getByText("+$350.00")).toBeInTheDocument(); // sell
    });
 
    it("renders quantity for each row", async () => {
      render(<History />);
      await screen.findByText("New Balance 550");
      expect(screen.getByText("Qty 2")).toBeInTheDocument();
    });
  });
 
  // ── Filter buttons ───────────────────────────────────────────────────────
  describe("filter controls", () => {
    it("renders All, Purchases and Sales filter buttons", async () => {
      render(<History />);
      expect(await screen.findByRole("button", { name: /^all$/i })).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /purchases/i })).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /sales/i })).toBeInTheDocument();
    });
 
    it("All filter is active by default", async () => {
      render(<History />);
      const allBtn = await screen.findByRole("button", { name: /^all$/i });
      expect(allBtn).toHaveClass("active");
    });
 
    it("filters to purchases only", async () => {
      render(<History />);
      const purchasesBtn = await screen.findByRole("button", { name: /purchases/i });
      fireEvent.click(purchasesBtn);
 
      expect(screen.getByText("Nike Air Force 1")).toBeInTheDocument();
      expect(screen.getByText("New Balance 550")).toBeInTheDocument();
      expect(screen.queryByText("Jordan 1 Retro High")).not.toBeInTheDocument();
      expect(screen.queryByText("Yeezy Boost 350")).not.toBeInTheDocument();
    });
 
    it("filters to sales only", async () => {
      render(<History />);
      const salesBtn = await screen.findByRole("button", { name: /sales/i });
      fireEvent.click(salesBtn);
 
      expect(screen.getByText("Jordan 1 Retro High")).toBeInTheDocument();
      expect(screen.getByText("Yeezy Boost 350")).toBeInTheDocument();
      expect(screen.queryByText("Nike Air Force 1")).not.toBeInTheDocument();
      expect(screen.queryByText("New Balance 550")).not.toBeInTheDocument();
    });
 
    it("returns to all transactions when All is clicked again", async () => {
      render(<History />);
      const salesBtn = await screen.findByRole("button", { name: /sales/i });
      fireEvent.click(salesBtn);
 
      const allBtn = screen.getByRole("button", { name: /^all$/i });
      fireEvent.click(allBtn);
 
      expect(screen.getByText("Nike Air Force 1")).toBeInTheDocument();
      expect(screen.getByText("Jordan 1 Retro High")).toBeInTheDocument();
    });
  });
 
  // ── Sort ─────────────────────────────────────────────────────────────────
  describe("sort control", () => {
    it("renders the sort select", async () => {
      render(<History />);
      expect(await screen.findByRole("combobox")).toBeInTheDocument();
    });
 
    it("defaults to newest first", async () => {
      render(<History />);
      const select = await screen.findByRole("combobox");
      expect(select).toHaveValue("date-desc");
    });
 
    it("sorts by amount descending", async () => {
      render(<History />);
      const select = await screen.findByRole("combobox");
      fireEvent.change(select, { target: { value: "amount-desc" } });
 
      const titles = screen.getAllByText(/Nike|Jordan|New Balance|Yeezy/);
      expect(titles[0]).toHaveTextContent("Jordan 1 Retro High"); // $350 highest
    });
 
    it("sorts by amount ascending", async () => {
      render(<History />);
      const select = await screen.findByRole("combobox");
      fireEvent.change(select, { target: { value: "amount-asc" } });
 
      const titles = screen.getAllByText(/Nike|Jordan|New Balance|Yeezy/);
      expect(titles[0]).toHaveTextContent("New Balance 550"); // $90 lowest
    });
  });
 
  // ── Auth & error states ──────────────────────────────────────────────────
  describe("auth and error handling", () => {
    it("shows error when no token is in localStorage", async () => {
      localStorage.removeItem("token");
      render(<History />);
      expect(await screen.findByText(/must be signed in/i)).toBeInTheDocument();
    });
 
    it("shows session expired message on 401", async () => {
      global.fetch = vi.fn().mockResolvedValue({ ok: false, status: 401 });
      render(<History />);
      expect(await screen.findByText(/session expired/i)).toBeInTheDocument();
    });
 
    it("shows generic error on server failure", async () => {
      global.fetch = vi.fn().mockRejectedValue(new Error("Network error"));
      render(<History />);
      expect(await screen.findByText(/failed to load/i)).toBeInTheDocument();
    });
 
    it("shows empty state when API returns no transactions", async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => [],
      });
      render(<History />);
      expect(await screen.findByText(/no transactions found/i)).toBeInTheDocument();
    });
  });
 
  // ── API call ─────────────────────────────────────────────────────────────
  /*describe("API integration", () => {
    it("calls /api/history on mount", async () => {
      render(<History />);
      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith(
          "/api/history",
          expect.objectContaining({
            headers: expect.objectContaining({
              Authorization: "Bearer mock-jwt-token",
            }),
          })
        );
      });
    });
  });*/
 
});