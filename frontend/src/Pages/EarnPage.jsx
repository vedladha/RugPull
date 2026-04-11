import { useState, useEffect } from "react";
import { useAuth } from "../Auth/auth-context"; // Adjust path if necessary
import "../style/earn-page.css";

export default function EarnPage() {
  const { user } = useAuth();
  const API = "http://localhost:3001";

  const [canClaim, setCanClaim] = useState(false);
  const [isClaiming, setIsClaiming] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (user) {
      fetch(`${API}/daily`, { credentials: "include" })
        .then(res => res.json())
        .then(data => {
          if (data.status) {
            // If claimed is false, the user CAN claim it
            setCanClaim(!data.status.claimed);
          }
        })
        .catch(err => console.error("Error fetching daily status:", err))
        .finally(() => setLoading(false));
    }
  }, [user]);

  const handleClaim = async () => {
    setIsClaiming(true);
    setError(null); // Reset error state on new attempt

    try {
      const response = await fetch(`${API}/daily/claim`, { credentials: "include" });

      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        throw new Error(errData.error || "Failed to claim reward.");
      }

      // On success, hide the banner completely
      setCanClaim(false);

      // TODO: If you need to update the wallet balance in context, call it here
      // e.g., updateWalletBalance();

    } catch (err) {
      setError(err.message);
    } finally {
      setIsClaiming(false);
    }
  };

  // 1. Logged Out View
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

  // 2. Logged In View
  return (
    <div className="earn-page">
      <div className="earn-header">
        <span className="hero-tag">Rewards</span>
        <h1>Earn $RPC</h1>
      </div>

      {loading ? (
        <div className="loading">Checking reward status...</div>
      ) : canClaim ? (
        // Banner is present and dynamically changes class if there's an error
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
        // Render a small message (or nothing at all) if the reward is already claimed
        <p className="earn-auth-msg">
          You have already claimed your reward today. Come back tomorrow!
        </p>
      )}
    </div>
  );
}