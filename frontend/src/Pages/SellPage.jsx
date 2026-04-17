import { useState, useEffect, useRef } from "react";
import { useAuth } from "../Auth/auth-context";
import ImageUploadBox from '../Components/ImageUploadBox';
import "../style/sell-page.css";

export default function SellPage() {
  const { user } = useAuth();
  const API = "http://localhost:3001";

  const formRef = useRef(null);

  // --- Form & Submission State ---
  const [form, setForm] = useState({ 
    title: "", 
    bio: "", 
    price: "", 
    quantity: "1", 
    image: null,
    existingImageUrl: null
  });
  
  // Track the original state for dirty checking (aborting unchanged PUTs)
  const [originalItem, setOriginalItem] = useState(null);
  
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");

  // --- Inventory & Edit State ---
  const [items, setItems] = useState([]);
  const [fetchingItems, setFetchingItems] = useState(true);
  const [editItemId, setEditItemId] = useState(null);

  const isEditMode = Boolean(editItemId);

  useEffect(() => {
    if (!user) return;

    const fetchMyItems = async () => {
      try {
        const response = await fetch(`${API}/items/me`, {
          credentials: "include",
        });

        if (!response.ok) {
          if (response.status === 404) return setItems([]);
          throw new Error("Failed to load inventory.");
        }

        const data = await response.json();
        setItems(data.items || []);
      } catch (err) {
        console.error(err);
      } finally {
        setFetchingItems(false);
      }
    };

    fetchMyItems();
  }, [user, API]);

  const handleFileChange = (field, file) => {
    if (!file) return;

    const MAX_SIZE = 5 * 1024 * 1024; // 5MB
    if (file.size > MAX_SIZE) {
      setErrors((prev) => ({
        ...prev,
        image: "This file is too large! Please choose an image under 5MB."
      }));
      return;
    }

    setForm((prev) => ({ 
      ...prev, 
      image: file, 
      existingImageUrl: null 
    }));
    
    if (errors.image) setErrors((prev) => ({ ...prev, image: null }));
    if (successMsg) setSuccessMsg("");
  };

  const validate = () => {
    const errs = {};
    if (!form.title.trim()) errs.title = "Title is required";
    if (!form.bio.trim()) errs.bio = "Description is required";
    if (form.price === "" || isNaN(form.price) || parseFloat(form.price) <= 0)
      errs.price = "Enter a valid price";
    if (form.quantity === "" || !Number.isInteger(Number(form.quantity)) || Number(form.quantity) < 0) {
      errs.quantity = "Enter a valid quantity";
    }
    return errs;
  };

  const handleChange = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: null }));
    if (successMsg) setSuccessMsg("");
  };

  const handleSubmit = async () => {
    const errs = validate();
    if (Object.keys(errs).length > 0) return setErrors(errs);

    // --- Dirty Checking / Abort Logic ---
    if (isEditMode && originalItem) {
      const firstImage = originalItem.images?.[0]?.imageUrl;
      const originalImageUrl = firstImage ? `${API}${firstImage}` : null;

      const isUnchanged = 
        form.title.trim() === originalItem.name &&
        form.bio.trim() === originalItem.description &&
        parseFloat(form.price) === originalItem.price &&
        Number(form.quantity) === (originalItem.stock || 0) &&
        form.image === null && // No new file selected
        form.existingImageUrl === originalImageUrl; // Image not removed/changed

      if (isUnchanged) {
        setSuccessMsg("No changes were made.");
        return; // Abort the fetch request
      }
    }

    setLoading(true);
    setErrors({});
    setSuccessMsg("");

    try {
      const url = isEditMode ? `${API}/items/${editItemId}` : `${API}/items`;
      const method = isEditMode ? "PUT" : "POST";

      const itemPayload = {
        name: form.title.trim(),
        description: form.bio.trim(),
        price: parseFloat(form.price),
        stock: Number(form.quantity),
      };

      const formData = new FormData();
      formData.append("item", new Blob([JSON.stringify(itemPayload)], { type: "application/json" }));
      
      if (form.image) {
        formData.append("file", form.image);
      }

      const response = await fetch(url, {
        method: method,
        credentials: "include",
        body: formData,
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || "Failed to process request.");
      }

      const responseData = await response.json();
      const savedItem = responseData.item;

      if (isEditMode) {
        setItems(items.map(item => item?.itemId === editItemId ? savedItem : item));
        setSuccessMsg("Listing successfully updated!");
      } else {
        setItems([...items, savedItem]);
        setSuccessMsg("New listing successfully posted!");
      }

      handleResetForm();
    } catch (err) {
      setErrors({ submit: err.message || "Failed to post listing. Please try again." });
    } finally {
      setLoading(false);
    }
  };

  const handleResetForm = () => {
    setForm({ title: "", bio: "", price: "", quantity: "1", image: null, existingImageUrl: null });
    setOriginalItem(null);
    setErrors({});
    setEditItemId(null);
  };

  const handleEditClick = (item) => {
    if (!item) return;
    setEditItemId(item.itemId);
    setOriginalItem(item);
    
    const firstImage = item.images && item.images.length > 0 ? item.images[0].imageUrl : null;
    
    setForm({
      title: item.name || "",
      bio: item.description || "",
      price: item.price !== undefined ? item.price.toString() : "",
      quantity: item.stock !== undefined ? item.stock.toString() : "1",
      image: null,
      existingImageUrl: firstImage ? `${API}${firstImage}` : null
    });

    setErrors({});
    setSuccessMsg("");

    if (formRef.current) {
      formRef.current.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  };

  const handleDeleteClick = async (itemId) => {
    if (!window.confirm("Are you sure you want to delete this listing?")) return;

    try {
      const response = await fetch(`${API}/items/${itemId}`, {
        method: "DELETE",
        credentials: "include",
      });

      if (!response.ok) throw new Error("Failed to delete item.");

      setItems(items.filter((item) => item?.itemId !== itemId));

      if (editItemId === itemId) {
        handleResetForm();
      }
    } catch (err) {
      alert(err.message);
    }
  };

  if (!user) {
    return (
      <div className="sell-page">
        <div className="sell-header">
          <span className="hero-tag">Seller Dashboard</span>
          <h1>Marketplace</h1>
        </div>
        <p>Please sign in to manage your listings.</p>
      </div>
    );
  }

  return (
    <div className="sell-page">
      <div className="sell-header" ref={formRef}>
        <span className="hero-tag">Seller Dashboard</span>
        <h1>{isEditMode ? "Edit Listing" : "Add New Listing"}</h1>
        <p>
          {isEditMode
            ? "Update your item details below."
            : "Fill in the details to post a new item to the marketplace."}
        </p>
      </div>

      <div className="sell-form" style={{ marginBottom: "2rem" }}>
        {successMsg && (
          <div className="sell-success-banner" style={{
            background: "rgba(16, 185, 129, 0.1)", border: "1px solid rgba(16, 185, 129, 0.3)",
            color: "#10b981", padding: "1rem", borderRadius: "4px", textAlign: "center",
            fontFamily: "var(--font-mono)", fontSize: "0.85rem", marginBottom: "1rem"
          }}>
            {successMsg}
          </div>
        )}

        <div className="sell-field">
          <label className="sell-label">
            Title <span className="sell-char-count">{form.title.length}/80</span>
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
          <label className="sell-label">Item Photo</label>
          <ImageUploadBox 
            initialImage={form.existingImageUrl} 
            onImageUpload={(file) => handleFileChange("image", file)} 
          />
          {errors.image && <span className="sell-error-msg">{errors.image}</span>}
        </div>


        <div className="sell-field">
          <label className="sell-label">
            Description <span className="sell-char-count">{form.bio.length}/300</span>
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
              type="number" placeholder="0.00" min="0" step="0.01" value={form.price}
              onChange={(e) => handleChange("price", e.target.value === "" ? "" : Math.max(0, parseFloat(e.target.value)))}
            />
          </div>
          {errors.price && <span className="sell-error-msg">{errors.price}</span>}
        </div>

        <div className="sell-field">
          <label className="sell-label" htmlFor="sell-quantity">Quantity</label>
          <input
            id="sell-quantity"
            className={`sell-input sell-input-quantity ${errors.quantity ? "sell-input-error" : ""}`}
            type="number" placeholder="1" min="0" step="1" value={form.quantity}
            onChange={(e) => handleChange("quantity", e.target.value)}
          />
          {errors.quantity && <span className="sell-error-msg">{errors.quantity}</span>}
        </div>

        {errors.submit && <div className="sell-error-banner">{errors.submit}</div>}

        <div style={{ display: "flex", gap: "1rem", marginTop: "1rem" }}>
          <button className="btn-primary sell-submit" onClick={handleSubmit} disabled={loading} style={{ flex: 1 }}>
            {loading ? "Saving..." : isEditMode ? "Save Changes" : "Post Listing"}
          </button>

          {isEditMode && (
            <button className="btn-ghost" onClick={handleResetForm} disabled={loading}>
              Cancel
            </button>
          )}
        </div>
      </div>

      <div style={{ width: "100%", maxWidth: "800px", borderTop: "1px solid var(--border)", paddingTop: "3rem" }}>
        <h2 style={{ fontSize: "1.2rem", fontWeight: 700, marginBottom: "1.5rem" }}>Your Active Inventory</h2>

        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          {fetchingItems ? (
            <div className="loading">Loading inventory...</div>
          ) : items.length === 0 ? (
            <p style={{ color: "var(--muted)", fontFamily: "var(--font-mono)", fontSize: "0.85rem" }}>
              You don't have any active listings yet.
            </p>
          ) : (
            items.map((item) => {
              if (!item) return null;
              return (
                <div
                  key={item.itemId}
                  className="listing-card"
                  style={{
                    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
                    borderColor: editItemId === item.itemId ? "var(--amber)" : "var(--border)"
                  }}
                >
                  <div style={{ display: "flex", flexDirection: "column", gap: "0.4rem" }}>
                    <h3 className="listing-title" style={{ margin: 0 }}>
                      {item.name}
                      {editItemId === item.itemId && <span style={{ color: "var(--amber)", fontSize: "0.7rem", marginLeft: "0.5rem" }}>(Editing)</span>}
                    </h3>
                    <div style={{ display: "flex", gap: "1rem", fontFamily: "var(--font-mono)", fontSize: "0.75rem", color: "var(--muted)" }}>
                      <span className="listing-price" style={{ fontSize: "0.85rem" }}>${parseFloat(item.price || 0).toFixed(2)}</span>
                      <span>Stock: {item.stock}</span>
                    </div>
                  </div>

                  <div style={{ display: "flex", gap: "0.75rem" }}>
                    <button
                      className="btn-ghost"
                      style={{ padding: "0.5rem 1rem", fontSize: "0.75rem" }}
                      onClick={() => handleEditClick(item)}
                      disabled={editItemId === item.itemId}
                    >
                      Edit
                    </button>
                    <button
                      className="btn-ghost"
                      style={{ padding: "0.5rem 1rem", fontSize: "0.75rem", borderColor: "rgba(255, 107, 107, 0.3)", color: "#ff6b6b" }}
                      onClick={() => handleDeleteClick(item.itemId)}
                    >
                      Delete
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}