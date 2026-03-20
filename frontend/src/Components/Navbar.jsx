import { Link } from "react-router-dom";
import { useAuth } from "../Auth/auth-context";

export default function Navbar({ onSignInClick, currentPage }) {
  const { user, walletBalance, signOut } = useAuth();
  const [balance, setBalance] = useState(null);

  useEffect(() => {
    if (user) {
      walletBalance()
        .then((fetchedBalance) => setBalance(fetchedBalance))
        .catch((err) => console.error("Failed to load balance in nav:", err));
    }
  }, [user, walletBalance]);


  return (
    <nav>
      <div className="logo" style={{ cursor: "pointer" }}>
        <Link to="/" style={{ textDecoration: "inherit", color: "inherit" }}>
          $RPC Market
        </Link>
      </div>
      <div className="nav-links">
        <button
          className={`nav-btn ${currentPage === "listings" ? "nav-btn-active" : ""}`}
        >
          <Link
            to="/listings"
            style={{ textDecoration: "none", color: "inherit" }}
          >
            Marketplace
          </Link>
        </button>
        <button className="nav-btn">
          <Link to="/sell" style={{ textDecoration: "none", color: "inherit" }}>
            Sell
          </Link>
        </button>
        <button className="nav-btn">About</button>
      </div>
      {user && balance !== null && (
        <div className="nav-user">Balance: {balance.toFixed(2)} RPC</div>
      )}
      {user && <Link to="/profile" style={{ textDecoration: 'none', color: 'inherit' }}>
        <div className="nav-user">Hello, {user.displayName}!</div>
      </Link>
      }
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
