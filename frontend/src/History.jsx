import React, { useState, useEffect, useMemo } from "react";
import "./style/history.css";

const SORT_OPTIONS = [
  { value: "date-desc", label: "Newest first" },
  { value: "date-asc", label: "Oldest first" },
  { value: "amount-desc", label: "Amount: high → low" },
  { value: "amount-asc", label: "Amount: low → high" },
];

export default function History() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState("all");
  const [sort, setSort] = useState("date-desc");

  const API = "http://localhost:3001";

  useEffect(() => {
    const fetchHistory = async () => {
      try {
        setLoading(true);
        setError(null);

        const response = await fetch(`${API}/orders/all`, {
          method: "GET",
		  credentials: "include",
		  headers: {
            "Content-Type": "application/json",
          },
        });

        if (response.status === 401) {
          setError("You must be signed in to view your history.");
          return;
        }

        if (!response.ok) {
          throw new Error(`Server error: ${response.status}`);
        }

        const data = await response.json();
        setHistory(data.orders);
	console.log(data.orders);
      } catch (err) {
        setError("Failed to load transaction history.");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchHistory();
  }, []);

  const stats = useMemo(() => {
    const totalSpent = history
      .filter((i) => i.orderType === "buy")
      .reduce((sum, i) => sum + i.totalPrice, 0);
    const totalEarned = history
      .filter((i) => i.orderType === "sell")
      .reduce((sum, i) => sum + i.totalPrice, 0);
    return { totalSpent, totalEarned, net: totalEarned - totalSpent };
  }, [history]);

  const filtered = useMemo(() => {
    let result =
      filter === "all" ? history : history.filter((i) => i.type === filter);

    result = [...result].sort((a, b) => {
      switch (sort) {
        case "date-asc":
          return new Date(a.date) - new Date(b.date);
        case "date-desc":
          return new Date(b.date) - new Date(a.date);
        case "amount-asc":
          return a.amount - b.amount;
        case "amount-desc":
          return b.amount - a.amount;
        default:
          return 0;
      }
    });

    return result;
  }, [history, filter, sort]);

  const formatDate = (dateStr) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  const formatAmount = (amount) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(amount);

  if (loading) {
    return (
      <div className="history-page">
        <div className="history-loading">Loading transactions…</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="history-page">
        <div className="history-error">{error}</div>
      </div>
    );
  }

  return (
    <div className="history-page">
      {/* Header */}
      <div className="history-header">
        <div className="hero-tag">Account</div>
        <h1>Transaction History</h1>
        <p className="history-subtitle">
          Your complete record of purchases and sales.
        </p>
      </div>

      {/* Summary stats */}
      <div className="history-stats">
        <div className="history-stat">
          <span className="history-stat-val sell">
            {formatAmount(stats.totalEarned)}
          </span>
          <span className="history-stat-lbl">Total earned</span>
        </div>
        <div className="history-stat">
          <span className="history-stat-val buy">
            {formatAmount(stats.totalSpent)}
          </span>
          <span className="history-stat-lbl">Total spent</span>
        </div>
        <div className="history-stat">
          <span
            className={`history-stat-val ${stats.net >= 0 ? "sell" : "buy"}`}
          >
            {stats.net >= 0 ? "+" : ""}
            {formatAmount(stats.net)}
          </span>
          <span className="history-stat-lbl">Net</span>
        </div>
        <div className="history-stat">
          <span className="history-stat-val neutral">{history.length}</span>
          <span className="history-stat-lbl">Transactions</span>
        </div>
      </div>

      {/* Controls */}
      <div className="history-controls">
        <div className="filter-buttons">
          {["all", "buy", "sell"].map((f) => (
            <button
              key={f}
              className={filter === f ? "active" : ""}
              onClick={() => setFilter(f)}
            >
              {f === "all" ? "All" : f === "buy" ? "Purchases" : "Sales"}
            </button>
          ))}
        </div>

        <div className="sort-wrap">
          <label className="sort-label">Sort</label>
          <select
            className="sort-select"
            value={sort}
            onChange={(e) => setSort(e.target.value)}
          >
            {SORT_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* List */}
      <div className="history-list">
        {filtered.length === 0 ? (
          <p className="no-history">No transactions found.</p>
        ) : (
          filtered.map((item) => (
            <div key={item.id} className={`history-item ${item.type}`}>
              <div className="item-type-chip">
                {item.orderType === "buy" ? "Purchase" : "Sale"}
              </div>
              <div className="item-header">
                <span className="item-title">{item.itemName}</span>
                <span className={`item-amount ${item.type}`}>
                  {item.orderType === "buy" ? "−" : "+"}
                  {formatAmount(item.totalPrice)}
                </span>
              </div>
              <div className="item-details">
                <span className="item-date">{formatDate(item.createdAt)}</span>
                <span className="item-qty">Qty {item.quantity}</span>
                <span className="item-counterparty">
                  {item.orderType === "buy"
                    ? `From ${item.sellerName}`
                    : `To ${item.buyerName}`}
                </span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

