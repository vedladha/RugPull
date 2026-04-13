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

  it("switches roulette bet modes and renders the number board", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ status: { claimed: true } }),
    }));

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /straight up/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: /straight up/i }));

    expect(screen.getByRole("group", { name: "Roulette number board" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Number 0" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Number 17" })).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: /dozens/i }));

    expect(screen.getByRole("button", { name: /1 - 12/i })).toBeInTheDocument();
  });

  it("requires a roulette selection before spinning", async () => {
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

    expect(screen.getByText("Choose a roulette bet before spinning.")).toBeInTheDocument();
  });

  it("shows the roulette spin result and calls updateUserBalance", async () => {
    vi.stubGlobal("matchMedia", vi.fn().mockImplementation(() => ({
      matches: true,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })));

    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ status: { claimed: true } }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          spin: {
            winningNumber: 17,
            winningColor: "BLACK",
            betType: "NUMBER",
            betValue: "17",
            wager: 5.0,
            payout: 180.0,
            netChange: 175.0,
            balance: 425.0,
            won: true,
            message: "You won. The wheel landed on 17 BLACK.",
          },
        }),
      });

    vi.stubGlobal("fetch", fetchMock);

    render(<EarnPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /straight up/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: /straight up/i }));
    await userEvent.click(screen.getByRole("button", { name: "Number 17" }));
    await userEvent.type(screen.getByPlaceholderText("Enter roulette wager"), "5");
    await userEvent.click(screen.getByRole("button", { name: "Spin Roulette" }));

    await waitFor(() => {
      expect(screen.getByText("You won 175.00 RPC. Ball landed on 17 BLACK.")).toBeInTheDocument();
      expect(screen.getByText("+175.00 RPC")).toBeInTheDocument();
      expect(screen.getByText("180.00 RPC")).toBeInTheDocument();
      expect(screen.getByText("425.00 RPC")).toBeInTheDocument();
      expect(mockUpdateUserBalance).toHaveBeenCalled();
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:3001/roulette/spin",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          wager: 5,
          betType: "NUMBER",
          betValue: "17",
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
      expect(screen.getByRole("button", { name: /columns/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: /columns/i }));
    await userEvent.click(screen.getByRole("button", { name: /column 3/i }));
    await userEvent.type(screen.getByPlaceholderText("Enter roulette wager"), "500");
    await userEvent.click(screen.getByRole("button", { name: "Spin Roulette" }));

    await waitFor(() => {
      expect(screen.getByText("Insufficient balance")).toBeInTheDocument();
    });
  });
});

        await waitFor(() => {
            expect(screen.getByText("Mint Error: Unauthorized mint attempt")).toBeInTheDocument();
        });
    });

    // 10. Ad Feature: Start Session and Display Player
    it("starts an ad session and displays the video player", async () => {
        const mockAdData = {
            session: {
                sessionId: "test-uuid-123",
                title: "Healing Potion Ad",
                durationSeconds: 15,
                rewardAmount: 5.0,
                videoUrl: "/ads/potion.mp4"
            }
        };

        const fetchMock = vi.fn()
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ status: { claimed: true } }) })
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve(mockAdData) });

        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);

        const startBtn = await screen.findByRole("button", { name: /watch ad to earn/i });
        await userEvent.click(startBtn);

        await waitFor(() => {
            expect(screen.getByText("Healing Potion Ad")).toBeInTheDocument();
            expect(screen.getByText(/Reward: 5/i)).toBeInTheDocument();
            const video = document.querySelector("video");
            expect(video).toHaveAttribute("src", expect.stringContaining("/ads/potion.mp4"));
            expect(video).toHaveAttribute("autoplay");
        });
    });

    // 11. Ad Feature: Success Lifecycle & Redesigned Card
    it("claims reward after video ends and shows the redesigned success card", async () => {
        const mockAdData = {
            session: {
                sessionId: "test-uuid-123",
                title: "Ad",
                durationSeconds: 1,
                rewardAmount: 5.0,
                videoUrl: "/v.mp4"
            }
        };

        const fetchMock = vi.fn()
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ status: { claimed: true } }) })
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve(mockAdData) })
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ message: "Success" }) });

        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);

        const startBtn = await screen.findByRole("button", { name: /watch ad to earn/i });
        await userEvent.click(startBtn);

        // Manually trigger the 'ended' event on the video tag
        const video = await waitFor(() => document.querySelector("video"));
        video.dispatchEvent(new Event("ended"));

        await waitFor(() => {
            // Check for elements from the new success-card redesign
            expect(screen.getByText("SUCCESS!")).toBeInTheDocument();
            expect(screen.getByText("+5")).toBeInTheDocument();
            expect(screen.getByText("Your tokens have been successfully deposited in your wallet.")).toBeInTheDocument();
            expect(screen.getByRole("button", { name: /watch another ad/i })).toBeInTheDocument();
        });
    });

    // 12. Ad Feature: Error Handling for Spoofing/Network
    it("shows error when the ad claim is rejected by the server", async () => {
        const mockAdData = {
            session: { sessionId: "bad-id", title: "Ad", durationSeconds: 30, rewardAmount: 5.0, videoUrl: "/v.mp4" }
        };

        const fetchMock = vi.fn()
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ status: { claimed: true } }) })
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve(mockAdData) })
            .mockResolvedValueOnce({
                ok: false,
                json: () => Promise.resolve({ error: "Ad completion spoofing detected." })
            });

        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);

        const startBtn = await screen.findByRole("button", { name: /watch ad to earn/i });
        await userEvent.click(startBtn);

        const video = await waitFor(() => document.querySelector("video"));
        video.dispatchEvent(new Event("ended"));

        await waitFor(() => {
            expect(screen.getByText("Claim Error: Ad completion spoofing detected.")).toBeInTheDocument();
        });
    });

    // 13. Ad Feature: Reset State
    it("resets back to idle state when 'Watch Another Ad' is clicked", async () => {
        const mockAdData = {
            session: { sessionId: "id", title: "Ad", durationSeconds: 1, rewardAmount: 5.0, videoUrl: "/v.mp4" }
        };

        const fetchMock = vi.fn()
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ status: { claimed: true } }) })
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve(mockAdData) })
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ message: "Success" }) });

        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);

        // Go through the flow to reach the success card
        await userEvent.click(await screen.findByRole("button", { name: /watch ad to earn/i }));
        const video = await waitFor(() => document.querySelector("video"));
        video.dispatchEvent(new Event("ended"));

        // Click the reset button on the success card
        const resetBtn = await screen.findByRole("button", { name: /watch another ad/i });
        await userEvent.click(resetBtn);

        // Verify we are back to the initial state
        expect(screen.getByRole("button", { name: /watch ad to earn/i })).toBeInTheDocument();
        expect(screen.queryByText("SUCCESS!")).toBeNull();
    });

    // 14. Coverage: Initial Load Failure (Lines 76-77)
    it("logs error to console when initial daily fetch fails", async () => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => { });
        vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("Initial Fetch Failed")));

        render(<EarnPage />);

        await waitFor(() => {
            expect(consoleSpy).toHaveBeenCalledWith("Error fetching daily status:", expect.any(Error));
        });

        consoleSpy.mockRestore();
    });

    // 15: 85-87 Coverage: handleWatchAd Network/Parse Failure
    it("handles fetch failure in handleWatchAd and sets error state", async () => {
        // 1. Mock the initial daily load so the page renders
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({ status: { claimed: true } })
            })
            // 2. Mock the ad start fetch to explode
            .mockRejectedValueOnce(new Error("Network Connection Refused"));

        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);

        // Click the button to trigger handleWatchAd
        const startBtn = await screen.findByRole("button", { name: /watch ad to earn/i });
        await userEvent.click(startBtn);

        // Verify the catch block logic executed
        await waitFor(() => {
            expect(screen.getByText(/Network Connection Refused/i)).toBeInTheDocument();
            // Verify status went back to idle by checking if the start button is visible again
            expect(screen.getByRole("button", { name: /watch ad to earn/i })).toBeInTheDocument();
        });
    });

    // 16. Coverage: handleDevMint Parse Error (Line 244)
    it("handles non-JSON error response in handleDevMint", async () => {
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ status: { claimed: true } }) })
            .mockResolvedValueOnce({
                ok: false,
                json: () => Promise.reject(new Error("Syntax Error")), // Force line 244 catch
            });

        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);
        await userEvent.type(screen.getByPlaceholderText("Amount (e.g. 500)"), "100");
        await userEvent.click(screen.getByRole("button", { name: "Execute Mint" }));

        await waitFor(() => {
            expect(screen.getByText("Mint Error: Failed to fund wallet.")).toBeInTheDocument();
        });
    });

    // 17. Coverage: handleWatchAd Server Error (Lines where !startRes.ok is true)
    it("throws and catches error when startRes is not ok", async () => {
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({ status: { claimed: true } })
            })
            // 2. Mock a server error response (e.g., 500 Internal Server Error)
            .mockResolvedValueOnce({
                ok: false,
                status: 500
            });

        vi.stubGlobal("fetch", fetchMock);

        render(<EarnPage />);

        const startBtn = await screen.findByRole("button", { name: /watch ad to earn/i });
        await userEvent.click(startBtn);

        // This triggers the "if (!startRes.ok)" block and throws your custom string
        await waitFor(() => {
            expect(screen.getByText("Failed to secure an ad session.")).toBeInTheDocument();
            // Confirm we went back to idle
            expect(screen.getByRole("button", { name: /watch ad to earn/i })).toBeInTheDocument();
        });
    });
});
