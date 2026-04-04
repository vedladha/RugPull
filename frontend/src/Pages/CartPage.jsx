import React, { useState, useEffect } from "react";
import { useAuth } from "../Auth/auth-context.js";
import "../style/cart-page.css";
import ListingModal from "../Components/ListingModal.jsx";

export default function CartPage() {
    const { user } = useAuth();
    const API = "http://localhost:3001";

    const [cart, setCart] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [selectedItem, setSelectedItem] = useState(null);

    useEffect(() => {
        const fetchCartData = async () => {
            setLoading(true);
            setError(null);

            try {
                const cartResponse = await fetch(`${API}/cart`, {
                    method: "GET",
                    credentials: "include"
                });

                if (!cartResponse.ok) throw new Error("Failed to fetch cart data");
                const cartJson = await cartResponse.json();

                // Safely extract the array
                const cartItems = Array.isArray(cartJson.cart) ? cartJson.cart : (Array.isArray(cartJson) ? cartJson : []);

                // Early exit if cart is empty to prevent failing the batch request
                if (cartItems.length === 0) {
                    setCart([]);
                    setLoading(false);
                    return;
                }

                const itemIds = cartItems.map(entry => entry.itemId);

                const itemsResponse = await fetch(`${API}/items/batch`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    credentials: "include",
                    body: JSON.stringify(itemIds),
                });

                if (!itemsResponse.ok) throw new Error("Failed to fetch batch items");
                const itemsJson = await itemsResponse.json();

                let fetchedItems = [];
                if (Array.isArray(itemsJson?.items?.items)) {
                    fetchedItems = itemsJson.items.items; // Handles the double wrap
                } else if (Array.isArray(itemsJson?.items)) {
                    fetchedItems = itemsJson.items;       // Handles a single wrap
                } else if (Array.isArray(itemsJson)) {
                    fetchedItems = itemsJson;             // Handles a raw array
                }

                const itemLookup = new Map(fetchedItems.map(item => [item.itemId, item]));

                const mergedCart = cartItems.map(cartItem => {
                    const itemDetails = itemLookup.get(cartItem.itemId) || { name: "Unknown Item", price: 0 };
                    return {
                        ...cartItem,
                        ...itemDetails
                    };
                });

                setCart(mergedCart);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        }

        if (user) {
            fetchCartData();
        }
    }, [user]);

    const handleUpdateQuantity = async (itemId, newQuantity) => {
        if (newQuantity < 1) return;

        try {
            const response = await fetch(`${API}/cart/${itemId}?quantity=${newQuantity}`, {
                method: "PUT",
                credentials: "include",
            });

            if (response.ok) {
                setCart((prevCart) =>
                    prevCart.map((item) =>
                        item.itemId === itemId ? { ...item, quantity: newQuantity } : item
                    )
                );
            } else {
                console.error("Failed to update item quantity");
            }
        } catch (err) {
            console.error("Error updating quantity:", err);
        }
    };

    const handleRemoveItem = async (itemId) => {
        try {
            const response = await fetch(`${API}/cart/${itemId}`, {
                method: "DELETE",
                credentials: "include",
            });

            if (response.ok) {
                setCart((prevCart) => prevCart.filter((item) => item.itemId !== itemId));
            } else {
                console.error("Failed to remove item");
            }
        } catch (err) {
            console.error("Error removing item:", err);
        }
    };

    const calculateTotal = () => {
        return cart.reduce((total, cartItem) => {
            const price = cartItem.price ? parseFloat(cartItem.price) : 0;
            return total + price * cartItem.quantity;
        }, 0).toFixed(2);
    };

    if (!user) {
        return (
            <div className="cart-page">
                <div className="cart-header">
                    <h1>Your Cart</h1>
                </div>
                <p className="no-listings">Please sign in to view your cart.</p>
            </div>
        );
    }

    if (loading) return <div className="loading">Loading your cart...</div>;
    if (error) return <div className="error">Error: {error}</div>;

    return (
        <div className="cart-page">
            <div className="cart-header">
                <h1>Your Cart</h1>
            </div>

            {cart.length === 0 ? (
                <div className="no-listings">Your cart is currently empty.</div>
            ) : (
                <div className="cart-container">
                    <div className="cart-items">
                        {cart.map((cartItem) => {
                            const name = cartItem.name || "Unknown Item";
                            const priceNum = parseFloat(cartItem.price) || 0;
                            const itemTotal = (priceNum * cartItem.quantity).toFixed(2);

                            return (
                                <div className="cart-row" key={cartItem.cartId || cartItem.itemId} onClick={() => setSelectedItem(cartItem)}>
                                    <div className="cart-item-info">
                                        <h3>{name}</h3>
                                        <p className="cart-item-price">
                                            ${priceNum.toFixed(2)}
                                        </p>
                                    </div>

                                    <div className="cart-item-actions">
                                        <div className="quantity-controls">
                                            <button onClick={() => handleUpdateQuantity(cartItem.itemId, cartItem.quantity - 1)}>
                                                -
                                            </button>
                                            <span>{cartItem.quantity}</span>
                                            <button onClick={() => handleUpdateQuantity(cartItem.itemId, cartItem.quantity + 1)}>
                                                +
                                            </button>
                                        </div>

                                        <p className="cart-item-total">${itemTotal}</p>

                                        <button
                                            className="remove-btn"
                                            onClick={() => handleRemoveItem(cartItem.itemId)}
                                            aria-label="Remove item"
                                        >
                                            ✕
                                        </button>
                                    </div>
                                </div>
                            );
                        })}
                    </div>

                    <div className="cart-summary">
                        <h2>Order Summary</h2>
                        <div className="summary-row">
                            <span>Subtotal</span>
                            <span>${calculateTotal()}</span>
                        </div>
                        <div className="modal-divider"></div>
                        <div className="summary-row total-row">
                            <span>Total</span>
                            <span>${calculateTotal()}</span>
                        </div>
                        <button className="btn-primary checkout-btn">
                            Proceed to Checkout
                        </button>
                    </div>
                    {
                        selectedItem && (
                            <ListingModal
                                listing={selectedItem}
                                onClose={() => setSelectedItem(null)}
                            />
                        )
                    }
                </div>
            )}
        </div>
    );
}