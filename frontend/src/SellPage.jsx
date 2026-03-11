import { useState } from "react";
import { useAuth } from "./Auth/AuthContext.jsx";

export default function SellPage() {
  const { user } = useAuth();

  // If no user is signed in, display a message prompting them to sign in
  if (!user) {
    return (
      <div className="sell-page">
        <div className="sell-header">
          <span className="hero-tag">Marketplace</span>
          <h1>List an Item</h1>
        </div>
        <p>Please sign in to list an item for sale.</p>
      </div>
    );
  }

  // State variables for form data, validation errors, submission status, and loading state
  const [form, setForm] = useState({ title: "", bio: "", price: "" });
  const [errors, setErrors] = useState({});
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);

  // validates that all fields are filled out and that price is a valid number greater than 0. 
  // Returns an object with error messages for any invalid fields
  const validate = () => {
    const errs = {};
    if (!form.title.trim()) errs.title = "Title is required";
    if (!form.bio.trim()) errs.bio = "Description is required";
    if (form.price === "" || isNaN(form.price) || parseFloat(form.price) <= 0)
      errs.price = "Enter a valid price";
    return errs;
  };

  // Handles changes to form fields. 
  // Updates the corresponding field in the form state and clears any existing error for that field
  const handleChange = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: null }));
  };

  // Handles form submission. Validates the form and if valid, sends a POST request to create a new listing.
  // Displays a success message and preview of the listing upon successful submission, or an error message if the request fails
  const handleSubmit = async () => {
    const errs = validate();
    if (Object.keys(errs).length > 0) return setErrors(errs);

    setLoading(true);
    try {
      await fetch("API_ENDPOINT/listings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: form.title.trim(),
          bio: form.bio.trim(),
          price: parseFloat(form.price),
        }),
      });
      setSubmitted(true);
    } catch (err) {
      setErrors({ submit: "Failed to post listing. Please try again." });
    } finally {
      setLoading(false);
    }
  };

  // Resets the form to its initial state, clearing all fields and errors, and allowing the user to post another listing
  const handleReset = () => {
    setForm({ title: "", bio: "", price: "" });
    setErrors({});
    setSubmitted(false);
  };

  // If listing has been validly submitted, display a success message and a preview of the listing. Otherwise, display the form for creating a new listing
  if (submitted) {
    return (
      <div className="sell-page">
        <div className="sell-success">
          <div className="sell-success-icon">✦</div>
          <h2>Listing Posted</h2>
          <p>Your item is now live on the marketplace.</p>
          <div className="sell-success-preview">
            <div className="listing-title">{form.title}</div>
            <div className="listing-bio">{form.bio}</div>
            <div className="listing-price">${parseFloat(form.price).toFixed(2)}</div>
          </div>
          <button className="btn-primary" onClick={handleReset}>
            Post Another
          </button>
        </div>
      </div>
    );
  }

  // Default view with the form for creating a new listing
  // Includes fields for title, description, and price
  return (
    <div className="sell-page">
      <div className="sell-header">
        <span className="hero-tag">Marketplace</span>
        <h1>List an Item</h1>
        <p>Fill in the details below to post your item for sale.</p>
      </div>

      <div className="sell-form">
        <div className="sell-field">
          <label className="sell-label">
            Title
            <span className="sell-char-count">{form.title.length}/80</span>
          </label>
          <input
            className={`sell-input ${errors.title ? "sell-input-error" : ""}`}
            type="text"
            placeholder="What are you selling?"
            maxLength={80}
            value={form.title}
            onChange={(e) => handleChange("title", e.target.value)}
          />
          {errors.title && <span className="sell-error-msg">{errors.title}</span>}
        </div>

        <div className="sell-field">
          <label className="sell-label">
            Description
            <span className="sell-char-count">{form.bio.length}/300</span>
          </label>
          <textarea
            className={`sell-input sell-textarea ${errors.bio ? "sell-input-error" : ""}`}
            placeholder="Describe the condition, specs, or any relevant details..."
            maxLength={300}
            value={form.bio}
            onChange={(e) => handleChange("bio", e.target.value)}
          />
          {errors.bio && <span className="sell-error-msg">{errors.bio}</span>}
        </div>

        <div className="sell-field sell-field-price">
          <label className="sell-label">Price</label>
          <div className="sell-price-wrap">
            <span className="sell-price-symbol">RPC</span>
            <input
              className={`sell-input sell-input-price ${errors.price ? "sell-input-error" : ""}`}
              type="number"
              placeholder="0.00"
              min="0"
              step="0.01"
              value={form.price}
              onChange={(e) =>
                handleChange(
                  "price",
                  e.target.value === "" ? "" : Math.max(0, e.target.value)
                )
              }
            />
          </div>
          {errors.price && <span className="sell-error-msg">{errors.price}</span>}
        </div>

        {errors.submit && <div className="sell-error-banner">{errors.submit}</div>}

        <button className="btn-primary sell-submit" onClick={handleSubmit} disabled={loading}>
          {loading ? "Posting..." : "Post Listing"}
        </button>
      </div>
    </div>
  );
}