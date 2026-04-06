import { useState, useEffect, useRef } from "react";
import { useAuth } from "../Auth/auth-context";
import { useNavigate, useLocation } from "react-router-dom";
import "../style/auth-page.css";

export default function AuthPage() {
  const { signIn, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const isSignupRoute = location.pathname.includes("signup");
  const [isSignUp, setIsSignUp] = useState(isSignupRoute);

  const displayNameRef = useRef(null);
  const emailRef = useRef(null);
  const passwordRef = useRef(null);

  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    setIsSignUp(location.pathname.includes("signup"));
    setError("");
  }, [location.pathname]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    // Extract the actual string values right at the moment of submission
    const displayName = displayNameRef.current?.value || "";
    const email = emailRef.current?.value || "";
    const password = passwordRef.current?.value || "";

    try {
      if (isSignUp) {
        if (!displayName.trim()) throw new Error("Display name is required");
        await register(displayName, email, password);
      } else {
        await signIn(email, password);
      }

      navigate("/");
    } catch (err) {
      setError(err.message || "An error occurred during authentication.");
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } finally {
      setIsLoading(false);
    }
  };

  const toggleMode = () => {
    const nextRoute = isSignUp ? "/login" : "/signup";
    navigate(nextRoute);
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-container">

        <div className="auth-header">
          <h1>{isSignUp ? "Create Account" : "Welcome Back"}</h1>
          <p>
            {isSignUp
              ? "Join the $RPC Market today."
              : "Sign in to your account to continue."}
          </p>
        </div>

        {error && (
          <div className="auth-banner-error">
            <span>⚠</span> {error}
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit}>

          {isSignUp && (
            <div className="auth-group">
              <label className="auth-label">Display Name</label>
              <input
                type="text"
                className="auth-input"
                placeholder="Market Trader 99"
                ref={displayNameRef}
                required={isSignUp}
              />
            </div>
          )}

          <div className="auth-group">
            <label className="auth-label">Email Address</label>
            <input
              type="email"
              className="auth-input"
              placeholder="you@example.com"
              ref={emailRef}
              required
            />
          </div>

          <div className="auth-group">
            <label className="auth-label">Password</label>
            <input
              type="password"
              className="auth-input"
              placeholder="••••••••"
              ref={passwordRef}
              required
            />
          </div>

          <button
            type="submit"
            className="auth-submit-btn"
            disabled={isLoading}
          >
            {isLoading ? "Processing..." : (isSignUp ? "Create Account" : "Sign In")}
          </button>

        </form>

        <div className="auth-footer">
          {isSignUp ? (
            <p>
              Already have an account?
              <button type="button" className="auth-toggle-link" onClick={toggleMode}>
                Sign in
              </button>
            </p>
          ) : (
            <p>
              New to $RPC Market?
              <button type="button" className="auth-toggle-link" onClick={toggleMode}>
                Create account
              </button>
            </p>
          )}
        </div>

      </div>
    </div>
  );
}