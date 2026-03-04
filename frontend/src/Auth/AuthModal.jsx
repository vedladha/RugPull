import { useState } from "react";
import { useAuth } from "./AuthContext.jsx";

export default function AuthModal({ initialSignUp = false, onClose }) {
  const { signIn, register } = useAuth();
  const [isSignUp, setIsSignUp] = useState(initialSignUp);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [error, setError] = useState(null);

  async function handleSubmit() {
    setError(null);
    try {
      if (isSignUp) {
        await register(displayName, email, password);
      } else {
        await signIn(email, password);
      }
      onClose();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2>{isSignUp ? "Create account" : "Welcome back"}</h2>
        <p>{isSignUp ? "Get started with $PRC Market." : "Sign in to your $RPC Market account."}</p>

        {isSignUp && (
          <>
            <div className="modal-label">Display Name</div>
            <input className="modal-input" type="text" placeholder="display name" value={displayName} onChange={(e) => setDisplayName(e.target.value)}/>
          </>
        )}

        <div className="modal-label">Email</div>
        <input
          className="modal-input"
          type="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <div className="modal-label">Password</div>
        <input
          className="modal-input"
          type="password"
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {error && <p style={{ color: "red", fontSize: "0.75rem", marginBottom: "1rem" }}>{error}</p>}

        <button className="modal-submit" onClick={handleSubmit}>
          {isSignUp ? "Create Account" : "Sign In"}
        </button>

        <div className="modal-footer">
          {isSignUp
            ? <><span>Already have an account? </span><a onClick={() => setIsSignUp(false)}>Sign in</a></>
            : <><span>New to $RPC Market? </span><a onClick={() => setIsSignUp(true)}>Create account</a></>
          }
        </div>
      </div>
    </div>
  );
}
