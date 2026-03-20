import { useState, useEffect } from "react";
import "./style/profile-page.css";
import { useAuth } from "./Auth/auth-context";

export default function ProfilePage() {
    const { user, profileDetails, updateProfile } = useAuth();
    const [displayName, setDisplayName] = useState("")
    const [email, setEmail] = useState("");
    const [bio, setBio] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [balance, setBalance] = useState(0);

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    // Load user data on component mount
    useEffect(() => {
        profileDetails()
            .then((fetchedDetails) => {
                const profileData = fetchedDetails.profile;

                setEmail(user.email)
                setDisplayName(profileData.displayName)
                setBio(profileData.bio)
                setBalance(-999.99)
            })
    }, [user, profileDetails]);

    const handleSave = async (e) => {
        e.preventDefault();

        setError("");
        setSuccess("");

        if (password && password !== confirmPassword) {
            setError("Passwords do not match!");
            window.scrollTo({ top: 0, behavior: 'smooth' });
            return;
        }

        try {
            await updateProfile(displayName, bio);

            setSuccess("Profile updated successfully");
            setPassword("");
            setConfirmPassword("");

            setTimeout(() => setSuccess(""), 4000);
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } catch (err) {
            setError(err.message);
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
        
    };

    return (
        <div className="profile-container">
            <div className="profile-header">
                <h1>Your Profile</h1>
                <p>Manage your account settings and view your wallet balance.</p>
            </div>

            <div className="profile-balance-card">
                <div className="balance-info">
                    <span className="balance-label">Current Balance</span>
                    <div className="balance-amount">
                        {balance.toFixed(2)} <span>$RPC</span>
                    </div>
                </div>
                {/* TODO: Add a link or modal trigger here to redirect to wallet/deposit if needed */}
                {/* TODO: When transaction history <button className="btn-ghost" style={{ padding: '0.6rem 1.5rem' }}>
                    View History
                </button> */}
            </div>

            {error && (
                <div className="message-banner error-banner">
                    <span className="banner-icon">⚠</span> {error}
                </div>
            )}

            {success && (
                <div className="message-banner success-banner">
                    <span className="banner-icon">✓</span> {success}
                </div>
            )}

            <form className="profile-form" onSubmit={handleSave}>
                <div className="form-group">
                    <label className="form-label">Username</label>
                    <input
                        type="text"
                        className="form-input"
                        value={displayName}
                        onChange={(e) => setDisplayName(e.target.value)}
                    />
                </div>

                <div className="form-group">
                    <label className="form-label">Email Address</label>
                    <input
                        type="email"
                        className="form-input"
                        value={email}
                        disabled
                        title="Email cannot be changed"
                    />
                </div>

                <div className="form-group">
                    <label className="form-label">Biography</label>
                    <textarea
                        className="form-textarea"
                        placeholder="Tell us about yourself..."
                        value={bio}
                        onChange={(e) => setBio(e.target.value)}
                    />
                </div>

                <div className="form-row">
                    <div className="form-group">
                        <label className="form-label">New Password</label>
                        <input
                            type="password"
                            className="form-input"
                            placeholder="Leave blank to keep current"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Confirm New Password</label>
                        <input
                            type="password"
                            className="form-input"
                            placeholder="Confirm your new password"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                        />
                    </div>
                </div>

                <div className="profile-actions">
                    <button type="submit" className="btn-primary">
                        Save Changes
                    </button>
                </div>
            </form>
        </div>
    );
}