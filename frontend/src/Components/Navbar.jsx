import { Link } from "react-router-dom";
import { useAuth } from "../Auth/AuthContext.jsx";

export default function Navbar({ onSignInClick, currentPage }) {
  const { user, signOut } = useAuth();
  return (
    <nav>
      <div
        className="logo"
        style={{ cursor: "pointer" }}
      >
        <Link to="/" style={{textDecoration: 'inherit', color: 'inherit'}}>$RPC Market</Link>
      </div>
      <div className="nav-links">
        <button
          className={`nav-btn ${currentPage === "listings" ? "nav-btn-active" : ""}`}
        >
          <Link to="/listings" style={{textDecoration: 'none', color: 'inherit'}}>Marketplace</Link>
        </button>
        <button className="nav-btn">Sell</button>
        <button className="nav-btn">About</button>
      </div>
      {user && <div className="nav-user">Hello, {user.displayName}!</div>}
      {user ? (
        <button className="nav-signin" onClick={signOut}>
          Sign Out
        </button>
      ) : (
        <button className="nav-signin" onClick={onSignInClick}>
          Sign In
        </button>
      )}
    </nav>
  );
}
