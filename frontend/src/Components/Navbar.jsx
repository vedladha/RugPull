import { useAuth } from "../Auth/useAuth";

export default function Navbar({ onSignInClick, onNavigate, currentPage }) {
  const { user, signOut } = useAuth();
  return (
    <nav>
      <div
        className="logo"
        onClick={() => onNavigate && onNavigate("home")}
        style={{ cursor: "pointer" }}
      >
        $RPC Market
      </div>
      <div className="nav-links">
        <button
          className={`nav-btn ${currentPage === "listings" ? "nav-btn-active" : ""}`}
          onClick={() => onNavigate && onNavigate("listings")}
        >
          Marketplace
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
