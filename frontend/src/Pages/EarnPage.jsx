import { useState, useEffect } from "react";
import { useAuth } from "../Auth/auth-context"; 
import "../style/earn-page.css";

export default function EarnPage() {
  const { user } = useAuth();
  const API = "http://localhost:3001";

  // Daily Reward State
  const [canClaim, setCanClaim] = useState(false);
  const [isClaiming, setIsClaiming] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Dev Minting State
  const [fundAmount, setFundAmount] = useState("");
  const [isFunding, setIsFunding] = useState(false);
  const [fundMsg, setFundMsg] = useState({ text: "", type: "" });

  useEffect(() => {
    if (user) {
      fetch(`${API}/daily`, { credentials: "include" })
        .then(res => res.json())
        .then(data => {
          if (data.status) {
            setCanClaim(!data.status.claimed);
          }
        })
        .catch(err => console.error("Error fetching daily status:", err))
        .finally(() => setLoading(false));
    }
  }, [user]);

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
      // TODO: updateWalletBalance();
    } catch (err) {
      setError(err.message);
    } finally {
      setIsClaiming(false);
    }
  };

  // --- Developer Function: Arbitrary Minting ---
  const handleDevMint = async () => {
    setFundMsg({ text: "", type: "" });
    const amount = parseFloat(fundAmount);

    if (isNaN(amount) || amount <= 0) {
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
      // TODO: updateWalletBalance();
    } catch (err) {
      setFundMsg({ text: `Mint Error: ${err.message}`, type: "error" });
    } finally {
      setIsFunding(false);
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

  return (
    <div className="earn-page">
      <div className="earn-header">
        <span className="hero-tag">Rewards</span>
        <h1>Earn $RPC</h1>
      </div>

      {loading ? (
        <div className="loading">Checking reward status...</div>
      ) : canClaim ? (
        <div className={`daily-banner ${error ? "banner-error" : ""}`}>
          <div className="banner-content">
            <h2>Daily Login Reward</h2>
            <p>
              {error
                ? `Error: ${error}`
                : "Check in today to claim your free 10.0 $RPC tokens!"}
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
        <p className="earn-auth-msg">
          You have already claimed your reward today. Come back tomorrow!
        </p>
      )}

      {/* --- Developer Minting Tool --- */}
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
            onChange={(e) => setFundAmount(e.target.value)}
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