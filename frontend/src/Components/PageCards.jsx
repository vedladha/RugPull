const pages = [
  {
    icon: "🛍️",
    name: "Marketplace",
    desc: "Browse thousands of listings across all categories",
    action: "listings",
  },
  {
    icon: "📦",
    name: "My Orders",
    desc: "Track purchases and manage your transactions",
    action: "orders",
  },
  {
    icon: "🏷️",
    name: "Sell",
    desc: "List your items and receive crypto payments",
    action: "sell",
  },
  {
    icon: "📊",
    name: "Portfolio",
    desc: "View your holdings and transaction history",
    action: "portfolio",
  },
];

export default function PageCards({ onCardClick, onNavigate }) {
  return (
    <div className="pages-section">
      <h2>Where do you want to go?</h2>
      <div className="pages-grid">
        {pages.map((p) => (
          <div
            className="page-card"
            key={p.name}
            onClick={() => onNavigate && onNavigate(p.action)}
          >
            <div className="page-icon"> {p.icon} </div>
            <div className="page-name">{p.name}</div>
            <div className="page-desc">{p.desc}</div>
            <div className="page-arrow">→</div>
          </div>
        ))}
      </div>
    </div>
  );
}
