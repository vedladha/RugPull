import { useEffect, useRef, useState } from "react";
import { useAuth } from "../Auth/auth-context";
import "../style/earn-page.css";

const API = "http://localhost:3001";
const SLOT_SYMBOLS = ["CHERRY", "LEMON", "BAR", "STAR", "SEVEN"];
const DEFAULT_REELS = ["CHERRY", "BAR", "SEVEN"];
const SPIN_TICK_MS = 90;
const REEL_STOP_DELAYS_MS = [240, 420, 620];
const SYMBOL_META = {
  CHERRY: {
    icon: "🍒",
    label: "Cherry",
    accentClass: "cherry",
  },
  LEMON: {
    icon: "🍋",
    label: "Lemon",
    accentClass: "lemon",
  },
  BAR: {
    icon: "BAR",
    label: "Bar",
    accentClass: "bar",
  },
  STAR: {
    icon: "★",
    label: "Star",
    accentClass: "star",
  },
  SEVEN: {
    icon: "7",
    label: "Seven",
    accentClass: "seven",
  },
};
const PAYOUT_ROWS = [
  { symbol: "SEVEN", multiplier: "x10" },
  { symbol: "STAR", multiplier: "x6" },
  { symbol: "BAR", multiplier: "x4" },
  { symbol: "CHERRY", multiplier: "x3" },
  { symbol: "LEMON", multiplier: "x2" },
];
const ROULETTE_RED_NUMBERS = new Set([
  1, 3, 5, 7, 9, 12, 14, 16, 18,
  19, 21, 23, 25, 27, 30, 32, 34, 36,
]);
const ROULETTE_WHEEL_ORDER = [
  0, 32, 15, 19, 4, 21, 2, 25, 17, 34,
  6, 27, 13, 36, 11, 30, 8, 23, 10, 5,
  24, 16, 33, 1, 20, 14, 31, 9, 22, 18,
  29, 7, 28, 12, 35, 3, 26,
];
const ROULETTE_SLICE_ANGLE = 360 / ROULETTE_WHEEL_ORDER.length;
const ROULETTE_WHEEL_SPIN_MS = 2200;
const ROULETTE_NUMBER_ROWS = Array.from({ length: 12 }, (_, rowIndex) => ([
  rowIndex * 3 + 1,
  rowIndex * 3 + 2,
  rowIndex * 3 + 3,
]));
const ROULETTE_BET_TYPES = [
  {
    type: "COLOR",
    label: "Color",
    desc: "Red or black",
  },
  {
    type: "PARITY",
    label: "Odd / Even",
    desc: "Even-money outside bet",
  },
  {
    type: "RANGE",
    label: "Low / High",
    desc: "1-18 or 19-36",
  },
  {
    type: "DOZEN",
    label: "Dozens",
    desc: "12-number blocks",
  },
  {
    type: "COLUMN",
    label: "Columns",
    desc: "Table columns",
  },
  {
    type: "NUMBER",
    label: "Straight Up",
    desc: "Single number",
  },
];
const ROULETTE_BET_OPTIONS = {
  COLOR: [
    {
      value: "RED",
      label: "Red",
      desc: "Pays 2x total",
      tone: "red",
    },
    {
      value: "BLACK",
      label: "Black",
      desc: "Pays 2x total",
      tone: "black",
    },
  ],
  PARITY: [
    {
      value: "ODD",
      label: "Odd",
      desc: "Pays 2x total",
      tone: "gold",
    },
    {
      value: "EVEN",
      label: "Even",
      desc: "Pays 2x total",
      tone: "ivory",
    },
  ],
  RANGE: [
    {
      value: "LOW",
      label: "1 - 18",
      desc: "Low numbers",
      tone: "green",
    },
    {
      value: "HIGH",
      label: "19 - 36",
      desc: "High numbers",
      tone: "blue",
    },
  ],
  DOZEN: [
    {
      value: "FIRST12",
      label: "1 - 12",
      desc: "Pays 3x total",
      tone: "green",
    },
    {
      value: "SECOND12",
      label: "13 - 24",
      desc: "Pays 3x total",
      tone: "blue",
    },
    {
      value: "THIRD12",
      label: "25 - 36",
      desc: "Pays 3x total",
      tone: "gold",
    },
  ],
  COLUMN: [
    {
      value: "FIRST",
      label: "Column 1",
      desc: "1, 4, 7...",
      tone: "green",
    },
    {
      value: "SECOND",
      label: "Column 2",
      desc: "2, 5, 8...",
      tone: "blue",
    },
    {
      value: "THIRD",
      label: "Column 3",
      desc: "3, 6, 9...",
      tone: "gold",
    },
  ],
};

function formatRpc(amount) {
  return Number(amount).toFixed(2);
}

function pickRandomSymbol() {
  return SLOT_SYMBOLS[Math.floor(Math.random() * SLOT_SYMBOLS.length)];
}

function prefersReducedMotion() {
  return typeof window !== "undefined"
    && typeof window.matchMedia === "function"
    && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
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

function buildRouletteMessage(spin) {
  const netChange = Number(spin.netChange ?? 0);
  const outcome = `${spin.winningNumber} ${spin.winningColor}`;

  if (netChange > 0) {
    return {
      text: `You won ${formatRpc(netChange)} RPC. Ball landed on ${outcome}.`,
      type: "success",
    };
  }
  if (netChange < 0) {
    return {
      text: `You lost ${formatRpc(Math.abs(netChange))} RPC. Ball landed on ${outcome}.`,
      type: "error",
    };
  }
  return {
    text: spin.message || `Spin complete. Ball landed on ${outcome}.`,
    type: "neutral",
  };
}

function getRouletteNumberColor(number) {
  if (number === 0) {
    return "green";
  }

  return ROULETTE_RED_NUMBERS.has(number) ? "red" : "black";
}

function getRouletteBetTypeLabel(betType) {
  return ROULETTE_BET_TYPES.find(({ type }) => type === betType)?.label || betType;
}

function getRouletteWheelTargetRotation(currentRotation, winningNumber) {
  const pocketIndex = ROULETTE_WHEEL_ORDER.indexOf(winningNumber);
  if (pocketIndex === -1) {
    return currentRotation;
  }

  const desiredRotation = -(pocketIndex * ROULETTE_SLICE_ANGLE);
  const currentNormalized = ((currentRotation % 360) + 360) % 360;
  const desiredNormalized = ((desiredRotation % 360) + 360) % 360;
  const adjustment = (currentNormalized - desiredNormalized + 360) % 360;

  return currentRotation - (5 * 360) - adjustment;
}

function getRouletteBetValueLabel(betType, betValue) {
  if (!betValue) {
    return "";
  }

  const optionMatch = ROULETTE_BET_OPTIONS[betType]?.find(({ value }) => value === betValue);
  if (optionMatch) {
    return optionMatch.label;
  }

  if (betType === "NUMBER") {
    return `Number ${betValue}`;
  }

  return betValue;
}

export default function EarnPage() {
  const { user, userBalance, updateUserBalance } = useAuth();
  const spinTimersRef = useRef([]);
  const rouletteSpinTimeoutRef = useRef(null);

  const [canClaim, setCanClaim] = useState(false);
  const [isClaiming, setIsClaiming] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [streak, setStreak] = useState(0);
  const [rewardAmount, setRewardAmount] = useState(10);

  // Dev Minting State
  const [fundAmount, setFundAmount] = useState("");
  const [isFunding, setIsFunding] = useState(false);
  const [fundMsg, setFundMsg] = useState({ text: "", type: "" });

  const [slotWager, setSlotWager] = useState("");
  const [isSpinning, setIsSpinning] = useState(false);
  const [slotMsg, setSlotMsg] = useState({ text: "", type: "" });
  const [slotResult, setSlotResult] = useState(null);
  const [displayedReels, setDisplayedReels] = useState(DEFAULT_REELS);
  const [settledReels, setSettledReels] = useState([true, true, true]);
  const [rouletteWager, setRouletteWager] = useState("");
  const [selectedRouletteBetType, setSelectedRouletteBetType] = useState("COLOR");
  const [selectedRouletteBetValue, setSelectedRouletteBetValue] = useState("");
  const [isRouletteSpinning, setIsRouletteSpinning] = useState(false);
  const [rouletteMsg, setRouletteMsg] = useState({ text: "", type: "" });
  const [rouletteResult, setRouletteResult] = useState(null);
  const [rouletteWheelRotation, setRouletteWheelRotation] = useState(0);
  const [rouletteWheelTransitionMs, setRouletteWheelTransitionMs] = useState(0);
  const selectedRouletteTypeMeta = ROULETTE_BET_TYPES.find(
    ({ type }) => type === selectedRouletteBetType,
  ) || ROULETTE_BET_TYPES[0];
  const activeRouletteOptions = ROULETTE_BET_OPTIONS[selectedRouletteBetType] || [];

  function clearSpinTimers() {
    for (const timerId of spinTimersRef.current) {
      window.clearInterval(timerId);
      window.clearTimeout(timerId);
    }
    spinTimersRef.current = [];
  }

  function clearRouletteSpinTimer() {
    if (rouletteSpinTimeoutRef.current) {
      window.clearTimeout(rouletteSpinTimeoutRef.current);
      rouletteSpinTimeoutRef.current = null;
    }
  }

  function beginSpinAnimation() {
    if (prefersReducedMotion()) {
      return;
    }

    clearSpinTimers();
    setSettledReels([false, false, false]);
    setDisplayedReels([pickRandomSymbol(), pickRandomSymbol(), pickRandomSymbol()]);

    const spinInterval = window.setInterval(() => {
      setDisplayedReels([pickRandomSymbol(), pickRandomSymbol(), pickRandomSymbol()]);
    }, SPIN_TICK_MS);

    spinTimersRef.current.push(spinInterval);
  }

  function finishSpinAnimation(finalReels) {
    if (prefersReducedMotion()) {
      clearSpinTimers();
      setDisplayedReels(finalReels);
      setSettledReels([true, true, true]);
      return Promise.resolve();
    }

    clearSpinTimers();
    let revealed = [false, false, false];

    return new Promise((resolve) => {
      const spinInterval = window.setInterval(() => {
        setDisplayedReels(
          finalReels.map((symbol, index) => (revealed[index] ? symbol : pickRandomSymbol())),
        );
      }, SPIN_TICK_MS);

      spinTimersRef.current.push(spinInterval);

      REEL_STOP_DELAYS_MS.forEach((delay, index) => {
        const stopTimer = window.setTimeout(() => {
          revealed = revealed.map((isSettled, reelIndex) => (
            reelIndex === index ? true : isSettled
          ));

          setSettledReels([...revealed]);
          setDisplayedReels(
            finalReels.map((symbol, reelIndex) => (
              revealed[reelIndex] ? symbol : pickRandomSymbol()
            )),
          );

          if (index === finalReels.length - 1) {
            clearSpinTimers();
            setDisplayedReels(finalReels);
            setSettledReels([true, true, true]);
            resolve();
          }
        }, delay);

        spinTimersRef.current.push(stopTimer);
      });
    });
  }

  function animateRouletteWheel(winningNumber) {
    clearRouletteSpinTimer();

    const targetRotation = getRouletteWheelTargetRotation(
        rouletteWheelRotation,
        winningNumber,
    );

    if (prefersReducedMotion()) {
      setRouletteWheelTransitionMs(0);
      setRouletteWheelRotation(targetRotation);
      return Promise.resolve();
    }

    setRouletteWheelTransitionMs(ROULETTE_WHEEL_SPIN_MS);
    setRouletteWheelRotation(targetRotation);

    return new Promise((resolve) => {
      rouletteSpinTimeoutRef.current = window.setTimeout(() => {
        rouletteSpinTimeoutRef.current = null;
        resolve();
      }, ROULETTE_WHEEL_SPIN_MS);
    });
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

  useEffect(() => () => {
    clearSpinTimers();
    clearRouletteSpinTimer();
  }, []);

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
      updateUserBalance();
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
      updateUserBalance();
    } catch (err) {
      setFundMsg({ text: `Mint Error: ${err.message}`, type: "error" });
    } finally {
      setIsFunding(false);
    }
  };

  const handleSpin = async () => {
    setSlotMsg({ text: "", type: "" });
    setSlotResult(null);

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
    beginSpinAnimation();

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
      await finishSpinAnimation(spin.reels);
      setSlotResult(spin);
      setSlotMsg(buildSpinMessage(spin));
      setSlotWager("");
    } catch (err) {
      clearSpinTimers();
      setDisplayedReels(DEFAULT_REELS);
      setSettledReels([true, true, true]);
      setSlotMsg({ text: err.message || "Failed to spin slot machine.", type: "error" });
    } finally {
      updateUserBalance();
      setIsSpinning(false);
    }
  };

  const handleRouletteSpin = async () => {
    setRouletteMsg({ text: "", type: "" });
    setRouletteResult(null);

    if (!selectedRouletteBetValue) {
      setRouletteMsg({ text: "Choose a roulette bet before spinning.", type: "error" });
      return;
    }

    if (!rouletteWager.trim()) {
      setRouletteMsg({ text: "Enter a wager before spinning.", type: "error" });
      return;
    }

    const wager = Number(rouletteWager);
    if (!Number.isFinite(wager) || wager <= 0) {
      setRouletteMsg({ text: "Enter a valid wager greater than 0.", type: "error" });
      return;
    }

    setIsRouletteSpinning(true);

    try {
      const response = await fetch(`${API}/roulette/spin`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
          wager,
          betType: selectedRouletteBetType,
          betValue: selectedRouletteBetValue,
        }),
      });

      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        throw new Error(errData.error || "Failed to spin roulette.");
      }

      const data = await response.json();
      const spin = data.spin;
      await animateRouletteWheel(spin.winningNumber);
      setRouletteResult(spin);
      setRouletteMsg(buildRouletteMessage(spin));
      setRouletteWager("");
      updateUserBalance();
    } catch (err) {
      setRouletteMsg({ text: err.message || "Failed to spin roulette.", type: "error" });
    } finally {
      setIsRouletteSpinning(false);
    }
  };

  const handleRouletteBetTypeChange = (betType) => {
    setSelectedRouletteBetType(betType);
    setSelectedRouletteBetValue("");
    setRouletteMsg({ text: "", type: "" });
  };

  const handleRouletteBetValueChange = (betValue) => {
    setSelectedRouletteBetValue(betValue);
    setRouletteMsg({ text: "", type: "" });
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

  return (
    <div className="earn-page">
      <div className="earn-header">
        <span className="hero-tag">Rewards</span>
        <h1>Earn $RPC</h1>
      </div>

      <div className="earn-balance-card">
        <span className="earn-balance-label">Wallet Balance</span>
        <div className="earn-balance-value">
          ${formatRpc(userBalance)} RPC
        </div>
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
          {PAYOUT_ROWS.map(({ symbol, multiplier }) => {
            const meta = SYMBOL_META[symbol];
            return (
              <span
                key={symbol}
                className={`slot-payout slot-payout-${meta.accentClass}`}
              >
                <span aria-hidden="true" className="slot-payout-icon">{meta.icon}</span>
                <span>{meta.label}</span>
                <strong>{multiplier}</strong>
              </span>
            );
          })}
        </div>

        <div className="slot-reels" aria-label="Slot machine reels">
          {displayedReels.map((reel, index) => {
            const meta = SYMBOL_META[reel];
            const isSettled = settledReels[index];
            return (
              <div
                className={`slot-reel slot-reel-${meta.accentClass} ${isSettled
                  ? "is-settled"
                  : "is-spinning"}`}
                key={`${index}-${reel}`}
              >
                <div className="slot-reel-track">
                  <span
                    aria-label={meta.label}
                    className="slot-reel-icon"
                    role="img"
                  >
                    {meta.icon}
                  </span>
                  <span className="slot-reel-name">{meta.label}</span>
                </div>
              </div>
            );
          })}
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

      <div className="slot-machine-card roulette-card">
        <div className="slot-machine-header">
          <span className="slot-machine-badge">Table</span>
          <h2>Roulette</h2>
        </div>

        <div
          aria-label="Roulette wheel"
          className={`roulette-wheel-stage ${isRouletteSpinning ? "is-spinning" : ""}`}
        >
          <div aria-hidden="true" className="roulette-wheel-pointer" />
          <div className="roulette-wheel-shell">
            <div
              className="roulette-wheel-face"
              style={{
                transform: `rotate(${rouletteWheelRotation}deg)`,
                transitionDuration: `${rouletteWheelTransitionMs}ms`,
              }}
            >
              {ROULETTE_WHEEL_ORDER.map((number, index) => {
                const color = getRouletteNumberColor(number);
                const isWinningPocket = rouletteResult?.winningNumber === number;
                return (
                  <div
                    className="roulette-wheel-pocket"
                    key={number}
                    style={{ "--pocket-angle": `${index * ROULETTE_SLICE_ANGLE}deg` }}
                  >
                    <span
                      className={`roulette-wheel-pocket-label roulette-wheel-pocket-${color} ${
                        isWinningPocket ? "is-winning" : ""
                      }`}
                    >
                      {number}
                    </span>
                  </div>
                );
              })}
              <div aria-hidden="true" className="roulette-wheel-center-ring" />
              <div aria-hidden="true" className="roulette-wheel-hub" />
            </div>
          </div>
        </div>

        <div
          aria-label="Roulette bet types"
          className="roulette-type-tabs"
          role="group"
        >
          {ROULETTE_BET_TYPES.map(({ type, label, desc }) => (
            <button
              aria-pressed={selectedRouletteBetType === type}
              className={`roulette-type-btn ${
                selectedRouletteBetType === type ? "is-selected" : ""
              }`}
              disabled={isRouletteSpinning}
              key={type}
              onClick={() => handleRouletteBetTypeChange(type)}
              type="button"
            >
              <span className="roulette-type-label">{label}</span>
              <span className="roulette-type-desc">{desc}</span>
            </button>
          ))}
        </div>

        <div className="roulette-selection-summary">
          <span className="slot-label">Current Bet</span>
          <strong>{getRouletteBetTypeLabel(selectedRouletteBetType)}</strong>
          <p className="slot-machine-desc">
            {selectedRouletteBetValue
              ? `Selected: ${getRouletteBetValueLabel(
                selectedRouletteBetType,
                selectedRouletteBetValue,
              )}`
              : `Choose a ${selectedRouletteTypeMeta.label.toLowerCase()} bet option below.`}
          </p>
        </div>

        {selectedRouletteBetType === "NUMBER" ? (
          <div className="roulette-number-board" role="group" aria-label="Roulette number board">
            <button
              aria-label="Number 0"
              aria-pressed={selectedRouletteBetValue === "0"}
              className={`roulette-number-pocket roulette-number-pocket-green ${
                selectedRouletteBetValue === "0" ? "is-selected" : ""
              }`}
              disabled={isRouletteSpinning}
              onClick={() => handleRouletteBetValueChange("0")}
              type="button"
            >
              0
            </button>

            <div className="roulette-number-grid">
              {ROULETTE_NUMBER_ROWS.map((row) => (
                row.map((number) => {
                  const color = getRouletteNumberColor(number);
                  return (
                    <button
                      aria-label={`Number ${number}`}
                      aria-pressed={selectedRouletteBetValue === String(number)}
                      className={`roulette-number-pocket roulette-number-pocket-${color} ${
                        selectedRouletteBetValue === String(number) ? "is-selected" : ""
                      }`}
                      disabled={isRouletteSpinning}
                      key={number}
                      onClick={() => handleRouletteBetValueChange(String(number))}
                      type="button"
                    >
                      {number}
                    </button>
                  );
                })
              ))}
            </div>
          </div>
        ) : (
          <div
            aria-label={`${selectedRouletteTypeMeta.label} options`}
            className="roulette-bet-options"
            role="group"
          >
            {activeRouletteOptions.map(({ value, label, desc, tone }) => (
              <button
                aria-pressed={selectedRouletteBetValue === value}
                className={`roulette-bet-btn roulette-tone-${tone} ${
                  selectedRouletteBetValue === value ? "is-selected" : ""
                }`}
                disabled={isRouletteSpinning}
                key={value}
                onClick={() => handleRouletteBetValueChange(value)}
                type="button"
              >
                <span className="roulette-bet-label">{label}</span>
                <span className="roulette-bet-desc">{desc}</span>
              </button>
            ))}
          </div>
        )}

        <div className="slot-controls">
          <label className="slot-label" htmlFor="roulette-wager">
            Wager
          </label>
          <div className="slot-input-row">
            <input
              id="roulette-wager"
              type="text"
              inputMode="decimal"
              className="slot-input"
              placeholder="Enter roulette wager"
              value={rouletteWager}
              onChange={(event) => setRouletteWager(normalizeWagerInput(event.target.value))}
              disabled={isRouletteSpinning}
            />
            <button
              className="slot-btn roulette-spin-btn"
              onClick={handleRouletteSpin}
              disabled={isRouletteSpinning}
            >
              {isRouletteSpinning ? "Spinning..." : "Spin Roulette"}
            </button>
          </div>
        </div>

        {rouletteMsg.text && (
          <div className={`slot-msg ${rouletteMsg.type}`}>
            {rouletteMsg.text}
          </div>
        )}

        {rouletteResult && (
          <div className="slot-result-grid">
            <div className="slot-result-card">
              <span>Bet Type</span>
              <strong>{getRouletteBetTypeLabel(rouletteResult.betType)}</strong>
            </div>
            <div className="slot-result-card">
              <span>Selection</span>
              <strong>
                {getRouletteBetValueLabel(rouletteResult.betType, rouletteResult.betValue)}
              </strong>
            </div>
            <div className="slot-result-card">
              <span>Winning Pocket</span>
              <strong
                className={
                  `roulette-chip roulette-chip-${rouletteResult.winningColor.toLowerCase()}`
                }
              >
                {rouletteResult.winningNumber} {rouletteResult.winningColor}
              </strong>
            </div>
            <div className="slot-result-card">
              <span>Payout</span>
              <strong>{formatRpc(rouletteResult.payout)} RPC</strong>
            </div>
            <div className="slot-result-card">
              <span>Net</span>
              <strong>{`${Number(rouletteResult.netChange) >= 0 ? "+" : ""}${formatRpc(
                rouletteResult.netChange,
              )} RPC`}</strong>
            </div>
            <div className="slot-result-card">
              <span>New Balance</span>
              <strong>{formatRpc(rouletteResult.balance)} RPC</strong>
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
