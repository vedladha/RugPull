import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../Auth/auth-context";
import "../style/components/navbar.css";

export default function Navbar() {
  const { user, userBalance, signOut } = useAuth();
  const navigate = useNavigate();

  // Helper function for active link styling
  const navLinkClass = ({ isActive }) => (isActive ? "nav-btn nav-btn-active" : "nav-btn");

  const handleSignOut = () => {
    signOut();
    navigate("/");
  };

  return (
    <nav className="navbar">
      {/* Left: Logo */}
      <div className="nav-logo">
        <Link to="/">
          $RPC Market
        </Link>
      </div>

      {/* Center: Core Navigation */}
      <div className="nav-center">
        <NavLink to="/listings" className={navLinkClass}>
          Marketplace
        </NavLink>
        <NavLink to="/sell" className={navLinkClass}>
          Sell
        </NavLink>
        <NavLink to="/earn" className={navLinkClass}>
          Earn
        </NavLink>
      </div>

      {/* Right: User Controls & Auth */}
      <div className="nav-right">
        {user ? (
          <>
            {userBalance !== null && (
              <div className="nav-balance">
                {Number(userBalance).toFixed(2)} RPC
              </div>
            )}
            
            <div className="nav-user-actions">
              <NavLink to="/wishlist" className={navLinkClass}>
                Wishlist
              </NavLink>
              <NavLink to="/cart" className={navLinkClass}>
                Cart
              </NavLink>
              <NavLink to="/profile" className={navLinkClass}>
                {user.displayName || "Profile"}
              </NavLink>
              
              <button className="nav-signout" onClick={handleSignOut}>
                Sign Out
              </button>
            </div>
          </>
        ) : (
          <div className="nav-auth-actions">
            <button className="nav-btn" onClick={() => navigate("/login")}>
              Sign In
            </button>
            <button className="nav-signin" onClick={() => navigate("/signup")}>
              Create Account
            </button>
          </div>
        )}
      </div>
    </nav>
  );
}