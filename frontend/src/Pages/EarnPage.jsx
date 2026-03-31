import { useState, useEffect } from "react";
import { useAuth } from "../Auth/auth-context"; // Adjust path if necessary
import "../style/earn-page.css"; // Adjust path if necessary

export default function EarnPage() {
  const { user } = useAuth();

  // State for the daily claim banner
  const [isClaiming, setIsClaiming] = useState(false);
  const [hasClaimedToday, setHasClaimedToday] = useState(false);
  const [rewardAmount, setRewardAmount] = useState(10); // Example static reward

  useEffect(() => {
    setRewardAmount(0);
    // our fetch endpoint goes here
    // ------------------------------------------------------------------------
    // API ENDPOINT: CHECK DAILY CLAIM STATUS
    // ------------------------------------------------------------------------
    // TODO: When the page loads, check if the user has already claimed today.
    // Recommended Method: GET /api/earn/daily-status
    // Headers: Authorization containing the user's JWT or Session
    // Example Response: { claimed: true, nextClaimAvailable: "2026-04-01T00:00:00Z" }
    //
    // if (user) {
    //   fetch('/api/earn/daily-status')
    //     .then(res => res.json())
    //     .then(data => setHasClaimedToday(data.claimed));
    // }
  }, [user]);

  const handleDailyClaim = async () => {
    setIsClaiming(true);
    try {
      // ------------------------------------------------------------------------
      // API ENDPOINT: PROCESS DAILY CLAIM
      // ------------------------------------------------------------------------
      // TODO: Submit the claim request to the backend.
      // Recommended Method: POST /api/earn/claim-daily
      // Headers: Authorization containing the user's JWT or Session
      // 
      // const response = await fetch('/api/earn/claim-daily', { method: 'POST' });
      // if (!response.ok) throw new Error("Claim failed");
      // const data = await response.json();

      // Simulating a successful network request for UI purposes
      await new Promise((resolve) => setTimeout(resolve, 800));
      setHasClaimedToday(true);

      // TODO: If you have a wallet balance context, trigger a refresh here.
      // e.g., refreshWalletBalance();

    } catch (error) {
      console.error("Failed to claim daily reward:", error);
      // Optional: Add a toast notification or error state here
    } finally {
      setIsClaiming(false);
    }
  };

  function earnCard(title, description, reward) {
    return (
      // This is for extra earning cards
      <div className="earn-grid">
        <div className="earn-card">
          <div className="earn-card-header">
            <span className="earn-card-title">{title}</span>
            <span className="earn-reward-badge">+{reward} $RPC</span>
          </div>
          <p className="earn-card-desc">
            {description}
          </p>
        </div>
      </div>
    )
  }

  // Restrict access to logged-in users only
  if (user) {
    return (
      <div className="earn-page">
        <div className="earn-header">
          <span className="hero-tag">Rewards</span>
          <h1>Earn $RPC</h1>
        </div>
        <p className="earn-auth-msg">Please sign in to view and claim your rewards.</p>
      </div>
    );
  }

  return (
    <div className="earn-page">
      <div className="earn-header">
        <span className="hero-tag">Rewards</span>
        <h1>Earn $RPC</h1>
        <p>Complete tasks and claim your daily tokens to build your wallet.</p>
      </div>

      {/* The Daily Claim Banner */}
      <div className="earn-banner">
        <div className="earn-banner-info">
          <h2 className="earn-banner-title">Daily Login Reward</h2>
          <p className="earn-banner-desc">
            Check in every 24 hours to claim your free {rewardAmount} $RPC tokens.
            Keep your streak alive to unlock bonuses!
          </p>
        </div>
        <button
          className="earn-claim-btn"
          onClick={handleDailyClaim}
          disabled={hasClaimedToday || isClaiming}
        >
          {isClaiming
            ? "Claiming..."
            : hasClaimedToday
              ? "Claimed Today ✓"
              : "Claim Tokens"}
        </button>
      </div>

      {/* Expandable Entries Section */}
      <div className="earn-tasks-section">
        <h2>More Ways to Earn</h2>
        <div className="earn-grid">
          {earnCard("Watch", "Watch an ads to earn", 5)}
          {earnCard("Complete Profile", "Fill out all of your profile information so everyone knows who you are!", 10)}
          {earnCard("First Sale", "Upon your first sale on RPC Market earn a bonus %5 on your sale", "5%")}
        </div>
      </div>
    </div>
  );
}