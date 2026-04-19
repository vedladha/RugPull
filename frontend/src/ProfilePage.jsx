import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./style/profile-page.css";
import { useAuth } from "./Auth/auth-context";
import SignInPrompt from "./Components/SignInPrompt";

export default function ProfilePage() {
    const { user, userBalance, profileDetails, updateProfile, changePassword } = useAuth();
    const navigate = useNavigate();
    
    const [displayName, setDisplayName] = useState("");
    const [email, setEmail] = useState("");
    const [bio, setBio] = useState("");
    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    // Load user data on component mount
    useEffect(() => {
        if (!user) return;
        
        profileDetails()
            .then((fetchedDetails) => {
                const profileData = fetchedDetails.profile;

                setEmail(user.email);
                setDisplayName(profileData.displayName || "");
                setBio(profileData.bio || "");
            });
    }, [user, profileDetails]);

    const handleSave = async (e) => {
        e.preventDefault();

        setError("");
        setSuccess("");

        const wantsPasswordChange = currentPassword || newPassword || confirmPassword;

        if (wantsPasswordChange && (!currentPassword || !newPassword || !confirmPassword)) {
            setError("Fill out all password fields to change your password.");
            window.scrollTo({ top: 0, behavior: 'smooth' });
            return;
        }

        if (newPassword && newPassword !== confirmPassword) {
            setError("Passwords do not match!");
            window.scrollTo({ top: 0, behavior: 'smooth' });
            return;
        }

        try {
            await updateProfile(displayName, bio);

            if (wantsPasswordChange) {
                await changePassword(currentPassword, newPassword);
            }

            setSuccess(
                wantsPasswordChange
                    ? "Profile and password updated successfully"
                    : "Profile updated successfully"
            );
            setCurrentPassword("");
            setNewPassword("");
            setConfirmPassword("");

            setTimeout(() => setSuccess(""), 4000);
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } catch (err) {
            setError(err.message);
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
    };

    if (!user) {
        return (
            <SignInPrompt
                tag="Profile"
                title="Your Profile"
                message="Please sign in to view or change your profile."
            />
        );
    }

    return (
        <div className="profile-page">
            <div className="profile-header">
                <span className="hero-tag">Account</span>
                <h1>Welcome, {user.displayName || "User"}</h1>
                <p className="profile-email">{user.email}</p>
            </div>

            <div className="profile-content">
                
                {/* --- Notifications --- */}
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

                {/* --- Account Overview & History Link --- */}
                <section className="profile-card">
                    <h2>Account Overview</h2>
                    <div className="details-grid">
                        <div className="detail-item">
                            <span className="detail-label">Current Balance</span>
                            <span className="detail-value highlight">
                                {userBalance !== null ? `${Number(userBalance).toFixed(2)} RPC` : "Loading..."}
                            </span>
                        </div>
                    </div>
                    
                    <div className="profile-history-section">
                        <h3>Order History</h3>
                        <p>Review your past purchases, track recent orders, and check your transaction history.</p>
                        <button className="btn-ghost" onClick={() => navigate("/history")}>
                            View Order History
                        </button>
                    </div>
                </section>

                {/* --- Profile Settings Form --- */}
                <section className="profile-card">
                    <h2>Profile Settings</h2>
                    <form className="profile-form" onSubmit={handleSave}>
                        <div className="form-group">
                            <label className="form-label">Username</label>
                            <input
                                type="text"
                                className="form-input"
                                value={displayName}
                                onChange={(e) => setDisplayName(e.target.value)}
                                placeholder="Choose a display name"
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

                        <div className="password-section">
                            <h3>Change Password</h3>
                            <p>Leave blank if you do not wish to change your password.</p>
                            
                            <div className="form-group">
                                <label className="form-label">Current Password</label>
                                <input
                                    type="password"
                                    className="form-input"
                                    placeholder="Enter your current password"
                                    value={currentPassword}
                                    onChange={(e) => setCurrentPassword(e.target.value)}
                                />
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label className="form-label">New Password</label>
                                    <input
                                        type="password"
                                        className="form-input"
                                        placeholder="Enter a new password"
                                        value={newPassword}
                                        onChange={(e) => setNewPassword(e.target.value)}
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
                        </div>

                        <div className="profile-actions">
                            <button type="submit" className="btn-primary" style={{ width: "100%" }}>
                                Save Changes
                            </button>
                        </div>
                    </form>
                </section>
            </div>
        </div>
    );
}