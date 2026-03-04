import { useAuth } from "../Auth/AuthContext.jsx"

export default function Navbar({ onSignInClick }) {
  const { user, signOut } = useAuth();
  return (
    <nav>
      <div className="logo">$RPC Market</div>
      <div className="nav-links">
        <button className="nav-btn">Marketplace</button>
        <button className="nav-btn">Sell</button>
        <button className="nav-btn">About</button>
      </div>
      {user && <div className="nav-user">Hello, {user.displayName}!</div>}
      {user
        ? <button className="nav-signin" onClick={signOut}>Sign Out</button>
        : <button className="nav-signin" onClick={onSignInClick}>Sign In</button>
      }
    </nav>
  );
}
