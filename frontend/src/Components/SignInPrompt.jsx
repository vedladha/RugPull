import { useNavigate } from "react-router-dom";
import "../style/components/sign-in-prompts.css";

export default function SignInPrompt({
    tag,
    title = "Sign In Required",
    message = "Please sign in to access this page."
}) {
    const navigate = useNavigate();

    return (
        <div className="sign-in-prompt-wrapper">
            <div className="sign-in-prompt-content">
                {tag && <span className="hero-tag">{tag}</span>}
                <h1>{title}</h1>
                <p className="sign-in-prompt-msg">{message}</p>

                <div className="sign-in-prompt-actions">
                    <button
                        className="btn-primary"
                        onClick={() => navigate("/login")}
                    >
                        Go to Sign In
                    </button>
                    <button
                        className="btn-ghost"
                        onClick={() => navigate("/signup")}
                    >
                        Create Account
                    </button>
                </div>
            </div>
        </div>
    );
}