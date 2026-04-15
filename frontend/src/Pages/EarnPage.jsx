import { useEffect, useState } from "react";
import { useAuth } from "../Auth/auth-context";
import "../style/earn-page.css";

const API = "http://localhost:3001";

function formatRpc(amount) {
  return Number(amount).toFixed(2);
}

function normalizeWagerInput(value) {
  const sanitized = value.replace(/[^\d.]/g, "");
  if (!sanitized) {
    return "";
  }

  const hasDecimalPoint = sanitized.includes(".");
  const [wholePart, ...decimalParts] = sanitized.split(".");
  const decimalPart = decimalParts.join("").slice(0, 2);

  if (!hasDecimalPoint) {
    return wholePart;
  }

  return `${wholePart || "0"}.${decimalPart}`;
}

function buildSpinMessage(spin) {
  const netChange = Number(spin.netChange ?? 0);
  if (netChange > 0) {
    return {
      text: `You won ${formatRpc(netChange)} RPC.`,
      type: "success",
    };
  }
  if (netChange < 0) {
    return {
      text: `You lost ${formatRpc(Math.abs(netChange))} RPC.`,
      type: "error",
    };
  }
  return {
    text: spin.message || "Spin complete.",
    type: "neutral",
  };
}

export default function EarnPage() {
  const { user, walletBalance } = useAuth();

  const [canClaim, setCanClaim] = useState(false);
  const [isClaiming, setIsClaiming] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [streak, setStreak] = useState(0);
  const [rewardAmount, setRewardAmount] = useState(10);

  const [balance, setBalance] = useState(null);
  const [balanceLoading, setBalanceLoading] = useState(false);
  const [balanceError, setBalanceError] = useState("");

  const [fundAmount, setFundAmount] = useState("");
  const [isFunding, setIsFunding] = useState(false);
  const [fundMsg, setFundMsg] = useState({ text: "", type: "" });

  const [slotWager, setSlotWager] = useState("");
  const [isSpinning, setIsSpinning] = useState(false);
  const [slotMsg, setSlotMsg] = useState({ text: "", type: "" });
  const [slotResult, setSlotResult] = useState(null);

  async function refreshBalance() {
    if (!user || !walletBalance) {
      return;
    }

    setBalanceLoading(true);
    setBalanceError("");
    try {
      const nextBalance = await walletBalance();
      setBalance(nextBalance);
    } catch (err) {
      setBalanceError(err.message || "Unable to load wallet balance.");
    } finally {
      setBalanceLoading(false);
    }
  }

  useEffect(() => {
    if (!user) {
      return;
    }

    fetch(`${API}/daily`, { credentials: "include" })
      .then((res) => res.json())
      .then((data) => {
        if (data.status) {
          setCanClaim(!data.status.claimed);
          setStreak(data.status.streak || 0);
          setRewardAmount(data.status.next_reward_amount || 10);
        }
      })
      .catch((err) => console.error("Error fetching daily status:", err))
      .finally(() => setLoading(false));
  }, [user]);

  useEffect(() => {
    if (!user || !walletBalance) {
      setBalance(null);
      setBalanceError("");
      return;
    }

    let cancelled = false;

    async function loadBalance() {
      setBalanceLoading(true);
      setBalanceError("");
      try {
        const nextBalance = await walletBalance();
        if (!cancelled) {
          setBalance(nextBalance);
        }
      } catch (err) {
        if (!cancelled) {
          setBalanceError(err.message || "Unable to load wallet balance.");
        }
      } finally {
        if (!cancelled) {
          setBalanceLoading(false);
        }
      }
    }

    loadBalance();

    return () => {
      cancelled = true;
    };
  }, [user, walletBalance]);

  const handleClaim = async () => {
    setIsClaiming(true);
    setError(null);

    try {
      const response = await fetch(`${API}/daily/claim`, { credentials: "include" });

      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        throw new Error(errData.error || "Failed to claim reward.");
      }

      setCanClaim(false);
      setStreak((prev) => prev + 1);
      await refreshBalance();
    } catch (err) {
      setError(err.message);
    } finally {
      setIsClaiming(false);
    }
  };

  const handleDevMint = async () => {
    setFundMsg({ text: "", type: "" });
    const amount = parseFloat(fundAmount);

    if (Number.isNaN(amount) || amount <= 0) {
      setFundMsg({ text: "Please enter a valid amount greater than 0.", type: "error" });
      return;
    }

    setIsFunding(true);

    try {
      const response = await fetch(`${API}/wallets/fund`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({ amount }),
      });

      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        throw new Error(errData.error || "Failed to fund wallet.");
      }

      setFundMsg({ text: `Successfully minted ${amount} $RPC!`, type: "success" });
      setFundAmount("");
      await refreshBalance();
    } catch (err) {
      setFundMsg({ text: `Mint Error: ${err.message}`, type: "error" });
    } finally {
      setIsFunding(false);
    }
  };

  const handleSpin = async () => {
    setSlotMsg({ text: "", type: "" });

    if (!slotWager.trim()) {
      setSlotMsg({ text: "Enter a wager before spinning.", type: "error" });
      return;
    }

    const wager = Number(slotWager);
    if (!Number.isFinite(wager) || wager <= 0) {
      setSlotMsg({ text: "Enter a valid wager greater than 0.", type: "error" });
      return;
    }

    setIsSpinning(true);

    try {
      const response = await fetch(`${API}/slots/spin`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({ wager }),
      });

      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        throw new Error(errData.error || "Failed to spin slot machine.");
      }

      const data = await response.json();
      const spin = data.spin;
      setSlotResult(spin);
      setSlotMsg(buildSpinMessage(spin));
      setBalance(Number(spin.balance));
      setBalanceError("");
      setSlotWager("");
    } catch (err) {
      setSlotMsg({ text: err.message || "Failed to spin slot machine.", type: "error" });
    } finally {
      setIsSpinning(false);
    }
  };

  if (!user) {
    return (
      <div className="earn-page">
        <div className="earn-header">
          <span className="hero-tag">Rewards</span>
          <h1>Earn $RPC</h1>
        </div>
        <p className="earn-auth-msg">Please sign in to view your rewards.</p>
      </div>
    );
  }

  const displayedReels = slotResult?.reels ?? ["?", "?", "?"];

  return (
    <div className="earn-page">
      <div className="earn-header">
        <span className="hero-tag">Rewards</span>
        <h1>Earn $RPC</h1>
      </div>

      <div className="earn-balance-card">
        <span className="earn-balance-label">Wallet Balance</span>
        <div className="earn-balance-value">
          {balanceLoading
            ? "Loading..."
            : balance !== null
              ? `${formatRpc(balance)} RPC`
              : "Unavailable"}
        </div>
        <p className="earn-balance-note">
          {balanceError || "Daily rewards, slot spins, and dev minting update this balance."}
        </p>
      </div>

      {loading ? (
        <div className="loading">Checking reward status...</div>
      ) : canClaim ? (
        <div className={`daily-banner ${error ? "banner-error" : ""}`}>
          <div className="banner-content">
            <div className="banner-title-row">
              <h2>Daily Login Reward</h2>
              {streak > 0 && <span className="streak-badge">🔥 {streak} Day Streak</span>}
            </div>
            <p>
              {error
                ? `Error: ${error}`
                : `Check in today to claim your free ${rewardAmount.toFixed(1)} $RPC tokens!`}
            </p>
          </div>

          <button
            className="btn-primary claim-btn"
            onClick={handleClaim}
            disabled={isClaiming}
          >
            {isClaiming ? "Claiming..." : "Claim Tokens"}
          </button>
        </div>
      ) : (
        <div className="earn-claimed-msg">
          <p className="earn-auth-msg">
            You have already claimed your reward today. Come back tomorrow!
          </p>
          {streak > 0 && (
            <div className="streak-badge claimed-streak">🔥 Current Streak: {streak} Days</div>
          )}
        </div>
      )}

      <div className="slot-machine-card">
        <div className="slot-machine-header">
          <span className="slot-machine-badge">Game</span>
          <h2>RPC Slot Machine</h2>
          <p className="slot-machine-desc">Match all three reels to win.</p>
        </div>

        <div className="slot-machine-payouts">
          <span>SEVEN x10</span>
          <span>STAR x6</span>
          <span>BAR x4</span>
          <span>CHERRY x3</span>
          <span>LEMON x2</span>
        </div>

        <div className="slot-reels" aria-label="Slot machine reels">
          {displayedReels.map((reel, index) => (
            <div className="slot-reel" key={`${reel}-${index}`}>
              {reel}
            </div>
          ))}
        </div>

        <div className="slot-controls">
          <label className="slot-label" htmlFor="slot-wager">
            Wager
          </label>
          <div className="slot-input-row">
            <input
              id="slot-wager"
              type="text"
              inputMode="decimal"
              className="slot-input"
              placeholder="Enter RPC wager"
              value={slotWager}
              onChange={(event) => setSlotWager(normalizeWagerInput(event.target.value))}
              disabled={isSpinning}
            />
            <button className="slot-btn" onClick={handleSpin} disabled={isSpinning}>
              {isSpinning ? "Spinning..." : "Spin Slots"}
            </button>
          </div>
        </div>

        {slotMsg.text && (
          <div className={`slot-msg ${slotMsg.type}`}>
            {slotMsg.text}
          </div>
        )}

        {slotResult && (
          <div className="slot-result-grid">
            <div className="slot-result-card">
              <span>Wager</span>
              <strong>{formatRpc(slotResult.wager)} RPC</strong>
            </div>
            <div className="slot-result-card">
              <span>Payout</span>
              <strong>{formatRpc(slotResult.payout)} RPC</strong>
            </div>
            <div className="slot-result-card">
              <span>Net</span>
              <strong>{`${Number(slotResult.netChange) >= 0 ? "+" : ""}${formatRpc(
                slotResult.netChange,
              )} RPC`}</strong>
            </div>
            <div className="slot-result-card">
              <span>New Balance</span>
              <strong>{formatRpc(slotResult.balance)} RPC</strong>
            </div>
          </div>
        )}
      </div>

      <div className="dev-sandbox">
        <div className="dev-sandbox-header">
          <span className="dev-badge">⚠️ DEV ONLY</span>
          <h3>Arbitrary Minting Tool</h3>
        </div>
        <p className="dev-desc">Force-fund your connected wallet. Not meant for production.</p>

        <div className="dev-input-group">
          <input
            type="number"
            className="dev-input"
            placeholder="Amount (e.g. 500)"
            value={fundAmount}
            onChange={(event) => setFundAmount(event.target.value)}
            disabled={isFunding}
            min="1"
            step="any"
          />
          <button
            className="dev-btn"
            onClick={handleDevMint}
            disabled={isFunding || !fundAmount}
          >
            {isFunding ? "Minting..." : "Execute Mint"}
          </button>
        </div>

        {fundMsg.text && (
          <div className={`dev-msg ${fundMsg.type}`}>
            {fundMsg.text}
          </div>
        )}
      </div>
    </div>
  );
}
