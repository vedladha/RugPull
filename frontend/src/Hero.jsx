import { useAuth } from "./Auth/auth-context.js";
import { useNavigate } from "react-router-dom";

export default function Hero() {
  const { user } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="hero">
      <h1>Purchase your favorite items using your favorite coin.</h1>
      <p>The open marketplace for digital and physical goods. No banks, no borders — just peer-to-peer trade.</p>
      <div className="hero-actions">
        <button className="btn-primary">Explore Marketplace</button>
        {!user && <button className="btn-ghost" onClick={() => {
          navigate("/signup")
        }}>
          Create Account
        </button>}
      </div>
    </div>
  );
}
