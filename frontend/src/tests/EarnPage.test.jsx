import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import EarnPage from "../Pages/EarnPage.jsx";

const mockUseAuth = vi.fn();
const mockUpdateUserBalance = vi.fn();

vi.mock("../Auth/auth-context", () => ({
  useAuth: () => mockUseAuth(),
}));

describe("EarnPage", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
    mockUpdateUserBalance.mockClear();
    // Default auth state for most tests
    mockUseAuth.mockReturnValue({
      user: { email: "test@example.com", id: 1 },
      userBalance: 250,
      updateUserBalance: mockUpdateUserBalance,
    });
  });

  it("shows logged out message when no user is present", () => {
    mockUseAuth.mockReturnValue({
      user: null,
      userBalance: null,
      updateUserBalance: mockUpdateUserBalance,
    });

    render(<EarnPage />);

    expect(screen.getByText("Please sign in to view your rewards.")).toBeInTheDocument();
    expect(screen.queryByText("Checking reward status...")).toBeNull();
  });

  it("shows loading state initially while fetching daily status", async () => {
    let resolve;
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise((responseResolver) => {
        resolve = responseResolver;
      })),
    );

    render(<EarnPage />);

    expect(screen.getByText("Checking reward status...")).toBeInTheDocument();

    resolve({ ok: true, json: () => Promise.resolve({ status: { claimed: true } }) });
    await waitFor(() => {
      expect(screen.queryByText("Checking reward status...")).toBeNull();
    });
  });

  it("displays claim banner, wallet balance, and slot controls for a signed-in user", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        status: { claimed: false, streak: 3, next_reward_amount: 14.0 },
      }),
    }));

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByText("🔥 3 Day Streak")).toBeInTheDocument();
      expect(
        screen.getByText("Check in today to claim your free 14.0 $RPC tokens!"),
      ).toBeInTheDocument();
      expect(screen.getByText("$250.00 RPC")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Spin Slots" })).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Spin Roulette" })).toBeInTheDocument();
    });
  });

  it("displays already claimed message and current streak when previously claimed", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        status: { claimed: true, streak: 5 },
      }),
    }));

    render(<EarnPage />);

    await waitFor(() => {
      expect(
        screen.getByText("You have already claimed your reward today. Come back tomorrow!"),
      ).toBeInTheDocument();
      expect(screen.getByText("🔥 Current Streak: 5 Days")).toBeInTheDocument();
      expect(screen.queryByRole("button", { name: "Claim Tokens" })).toBeNull();
    });
  });

  it("handles claim success and calls updateUserBalance", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          status: { claimed: false, streak: 2, next_reward_amount: 10.0 },
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({}),
      });

    vi.stubGlobal("fetch", fetchMock);

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Claim Tokens" })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: "Claim Tokens" }));

    await waitFor(() => {
      expect(screen.getByText("🔥 Current Streak: 3 Days")).toBeInTheDocument();
      expect(mockUpdateUserBalance).toHaveBeenCalled();
      expect(screen.queryByRole("button", { name: "Claim Tokens" })).toBeNull();
    });
  });

  it("shows error when claiming fails", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          status: { claimed: false, streak: 0, next_reward_amount: 10.0 },
        }),
      })
      .mockResolvedValueOnce({
        ok: false,
        json: () => Promise.resolve({ error: "Network error occurred" }),
      });

    vi.stubGlobal("fetch", fetchMock);

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Claim Tokens" })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: "Claim Tokens" }));

    await waitFor(() => {
      expect(screen.getByText("Error: Network error occurred")).toBeInTheDocument();
    });
  });

  it("shows error when dev mint amount is invalid or zero", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ status: { claimed: true } }),
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText("Amount (e.g. 500)")).toBeInTheDocument();
    });

    await userEvent.type(screen.getByPlaceholderText("Amount (e.g. 500)"), "-50");
    await userEvent.click(screen.getByRole("button", { name: "Execute Mint" }));

    expect(
      screen.getByText("Please enter a valid amount greater than 0."),
    ).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("successfully mints tokens via the developer tool and calls updateUserBalance", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ status: { claimed: true } }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({}),
      });

    vi.stubGlobal("fetch", fetchMock);

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText("Amount (e.g. 500)")).toBeInTheDocument();
    });

    await userEvent.type(screen.getByPlaceholderText("Amount (e.g. 500)"), "150");
    await userEvent.click(screen.getByRole("button", { name: "Execute Mint" }));

    await waitFor(() => {
      expect(screen.getByText("Successfully minted 150 $RPC!")).toBeInTheDocument();
      expect(mockUpdateUserBalance).toHaveBeenCalled();
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:3001/wallets/fund",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ amount: 150 }),
      }),
    );
  });

  it("shows error when dev minting fails", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ status: { claimed: true } }),
      })
      .mockResolvedValueOnce({
        ok: false,
        json: () => Promise.resolve({ error: "Unauthorized mint attempt" }),
      });

    vi.stubGlobal("fetch", fetchMock);

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText("Amount (e.g. 500)")).toBeInTheDocument();
    });

    await userEvent.type(screen.getByPlaceholderText("Amount (e.g. 500)"), "100");
    await userEvent.click(screen.getByRole("button", { name: "Execute Mint" }));

    await waitFor(() => {
      expect(screen.getByText("Mint Error: Unauthorized mint attempt")).toBeInTheDocument();
    });
  });

  it("requires a wager before spinning", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ status: { claimed: true } }),
    }));

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Spin Slots" })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: "Spin Slots" }));

    expect(screen.getByText("Enter a wager before spinning.")).toBeInTheDocument();
  });

  it("shows the slot spin result and calls updateUserBalance", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ status: { claimed: true } }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          spin: {
            reels: ["SEVEN", "SEVEN", "SEVEN"],
            wager: 10.0,
            payout: 100.0,
            netChange: 90.0,
            balance: 340.0,
            won: true,
            message: "You won!",
          },
        }),
      });

    vi.stubGlobal("fetch", fetchMock);

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Spin Slots" })).toBeInTheDocument();
    });

    await userEvent.type(screen.getByPlaceholderText("Enter RPC wager"), "10");
    await userEvent.click(screen.getByRole("button", { name: "Spin Slots" }));

    expect(screen.getByRole("button", { name: "Spinning..." })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("You won 90.00 RPC.")).toBeInTheDocument();
      expect(screen.getByText("+90.00 RPC")).toBeInTheDocument();
      // Original static balance in header
      expect(screen.getByText("$250.00 RPC")).toBeInTheDocument();
      // New balance from the spin result payload
      expect(screen.getByText("340.00 RPC")).toBeInTheDocument();
      expect(screen.getAllByLabelText("Seven")).toHaveLength(3);
      expect(mockUpdateUserBalance).toHaveBeenCalled();
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:3001/slots/spin",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ wager: 10 }),
      }),
    );
  });

  it("shows the backend error when a slot spin fails", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ status: { claimed: true } }),
      })
      .mockResolvedValueOnce({
        ok: false,
        json: () => Promise.resolve({ error: "Insufficient balance" }),
      });

    vi.stubGlobal("fetch", fetchMock);

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Spin Slots" })).toBeInTheDocument();
    });

    await userEvent.type(screen.getByPlaceholderText("Enter RPC wager"), "500");
    await userEvent.click(screen.getByRole("button", { name: "Spin Slots" }));

    await waitFor(() => {
      expect(screen.getByText("Insufficient balance")).toBeInTheDocument();
    });
  });

  it("caps the wager input at two decimal places", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ status: { claimed: true } }),
    }));

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText("Enter RPC wager")).toBeInTheDocument();
    });

    const wagerInput = screen.getByPlaceholderText("Enter RPC wager");
    await userEvent.type(wagerInput, "10.239");

    expect(wagerInput).toHaveValue("10.23");
  });

  it("renders slot machine tiles with real symbols before a spin", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ status: { claimed: true } }),
    }));

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByText("Match all three reels to win.")).toBeInTheDocument();
      expect(screen.getByLabelText("Cherry")).toBeInTheDocument();
      expect(screen.getByLabelText("Bar")).toBeInTheDocument();
      expect(screen.getByLabelText("Seven")).toBeInTheDocument();
    });
  });

  it("requires a roulette color before spinning", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ status: { claimed: true } }),
    }));

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Spin Roulette" })).toBeInTheDocument();
    });

    await userEvent.type(screen.getByPlaceholderText("Enter roulette wager"), "10");
    await userEvent.click(screen.getByRole("button", { name: "Spin Roulette" }));

    expect(screen.getByText("Choose red or black before spinning.")).toBeInTheDocument();
  });

  it("shows the roulette spin result and calls updateUserBalance", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ status: { claimed: true } }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          spin: {
            winningNumber: 19,
            winningColor: "RED",
            betType: "COLOR",
            betValue: "RED",
            wager: 10.0,
            payout: 20.0,
            netChange: 10.0,
            balance: 260.0,
            won: true,
            message: "You won. The wheel landed on 19 RED.",
          },
        }),
      });

    vi.stubGlobal("fetch", fetchMock);

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /red/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: /red/i }));
    await userEvent.type(screen.getByPlaceholderText("Enter roulette wager"), "10");
    await userEvent.click(screen.getByRole("button", { name: "Spin Roulette" }));

    await waitFor(() => {
      expect(screen.getByText("You won 10.00 RPC. Ball landed on 19 RED.")).toBeInTheDocument();
      expect(screen.getByText("19 RED")).toBeInTheDocument();
      expect(screen.getByText("+10.00 RPC")).toBeInTheDocument();
      expect(screen.getByText("260.00 RPC")).toBeInTheDocument();
      expect(mockUpdateUserBalance).toHaveBeenCalled();
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:3001/roulette/spin",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          wager: 10,
          betType: "COLOR",
          betValue: "RED",
        }),
      }),
    );
  });

  it("shows the backend error when a roulette spin fails", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ status: { claimed: true } }),
      })
      .mockResolvedValueOnce({
        ok: false,
        json: () => Promise.resolve({ error: "Insufficient balance" }),
      });

    vi.stubGlobal("fetch", fetchMock);

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /black/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: /black/i }));
    await userEvent.type(screen.getByPlaceholderText("Enter roulette wager"), "500");
    await userEvent.click(screen.getByRole("button", { name: "Spin Roulette" }));

    await waitFor(() => {
      expect(screen.getByText("Insufficient balance")).toBeInTheDocument();
    });
  });
});
