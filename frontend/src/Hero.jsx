import { useAuth } from "./Auth/useAuth";

export default function Hero({ onCreateAccountClick }) {
  const { user } = useAuth();

  return (
    <div className="hero">
        <h1>Purchase your favorite items using your favorite coin.</h1>
        <p>The open marketplace for digital and physical goods. No banks, no borders — just peer-to-peer trade.</p>
        <div className="hero-actions">
          <button className="btn-primary">Explore Marketplace</button>
          {!user && <button className="btn-ghost" onClick={onCreateAccountClick}>
            Create Account
          </button>}
        </div>
      </div>
  );
}
