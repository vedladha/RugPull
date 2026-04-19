import { useNavigate } from "react-router-dom";
import { useAuth } from "../Auth/auth-context";
import "../style/front-page.css";

export default function FrontPage() {
    const { user } = useAuth();
    const navigate = useNavigate();

    const handleNavigate = (path) => {
        navigate(path);
        window.scrollTo(0, 0);
    };

    return (
        <div className="front-page-wrapper">
            <section className="fp-hero">
                <div className="fp-hero-glow"></div>
                <span className="hero-tag">Welcome to the future of commerce</span>
                <h1>
                    Trade digital & physical goods with <em>$RPC</em>.
                </h1>

                <p>
                    The open marketplace built for a decentralized world. No banks, no borders — just frictionless peer-to-peer trade.
                </p>

                <div className="fp-hero-actions">
                    <button
                        className="btn-primary"
                        onClick={() => handleNavigate("/listings")}
                    >
                        Explore Marketplace
                    </button>

                    {!user ? (
                        <button
                            className="btn-ghost"
                            onClick={() => handleNavigate("/signup")}
                        >
                            Create Account
                        </button>
                    ) : (
                        <button
                            className="btn-ghost"
                            onClick={() => handleNavigate("/profile")}
                        >
                            Go to Dashboard
                        </button>
                    )}
                </div>
            </section>

            <section className="fp-features">
                <div className="fp-features-header">
                    <h2>What would you like to do?</h2>
                    <p>Everything you need to buy, sell, and earn in one place.</p>
                </div>

                <div className="fp-grid">
                    <div className="fp-card" onClick={() => handleNavigate("/listings")}>
                        <div className="fp-card-icon">🛍️</div>
                        <div className="fp-card-content">
                            <h3>Shop the Market</h3>
                            <p>Browse active listings, add items to your cart, and checkout using your $RPC balance.</p>
                        </div>
                        <div className="fp-card-arrow">→</div>
                    </div>

                    <div className="fp-card" onClick={() => handleNavigate("/sell")}>
                        <div className="fp-card-icon">🏷️</div>
                        <div className="fp-card-content">
                            <h3>List an Item</h3>
                            <p>Turn your assets into crypto. Create a listing in seconds and manage your active inventory.</p>
                        </div>
                        <div className="fp-card-arrow">→</div>
                    </div>

                    <div className="fp-card" onClick={() => handleNavigate("/earn")}>
                        <div className="fp-card-icon">🎮</div>
                        <div className="fp-card-content">
                            <h3>Earn $RPC</h3>
                            <p>Claim your daily login rewards or try your luck at the slots and roulette tables to grow your bag.</p>
                        </div>
                        <div className="fp-card-arrow">→</div>
                    </div>

                    <div className="fp-card" onClick={() => handleNavigate("/profile")}>
                        <div className="fp-card-icon">💼</div>
                        <div className="fp-card-content">
                            <h3>Your Wallet</h3>
                            <p>Check your $RPC balance, update your profile details, and manage your account security.</p>
                        </div>
                        <div className="fp-card-arrow">→</div>
                    </div>
                </div>
            </section>
        </div>
    );
}