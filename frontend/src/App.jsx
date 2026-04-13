import "./index.css";
import { Routes, Route, useNavigate } from "react-router-dom";
import Navbar from "./Components/Navbar.jsx";
import Hero from "./Hero.jsx";
import PageCards from "./Components/PageCards.jsx";
import Listings from "./Listings.jsx";
import SellPage from "./SellPage.jsx";
import ProfilePage from "./ProfilePage.jsx";
import Footer from "./Components/Footer.jsx";
import AuthPage from "./Pages/AuthPage.jsx";
import CartPage from "./Pages/CartPage";
import OrderPage from "./Pages/OrderPage.jsx";
import History from "./History.jsx";

export default function App() {
  // const [modal, setModal] = useState(null); // null | "signin" | "signup"
  const navigate = useNavigate();

  return (
    <>
      <Navbar />
      <Routes>
        <Route
          path="/"
          element={
            <>
              <Hero />
              <PageCards
                onCardClick={(action) => {
                  if (action === "marketplace") {
                    navigate("/listings");
                  } else if (action === "sell") {
                    navigate("/sell");
                  } else if (action === "history") {
                    navigate("/history");
                  }
                }}
              />
            </>
          }
        />
        <Route path="/sell" element={<SellPage />} />
        <Route path="/listings" element={<Listings />} />
        <Route path="/history" element={<History />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/login" element={<AuthPage />} />
        <Route
          path="/signup"
          element={
            // AuthPage checks path to decide what to show
            <AuthPage />
          }
        />
<<<<<<< HEAD
=======
        <Route path="/profile" element={
          <ProfilePage />
        } />
        <Route path="/cart" element={
          <CartPage />
        } />
        <Route path="/order" element={
          <OrderPage />
        } />
        <Route path="/login" element={
          <AuthPage />
        } />
        <Route path="/signup" element={
          // AuthPage checks path to decide what to show
          <AuthPage />
        } />
>>>>>>> main
      </Routes>
      <Footer />
    </>
  );
}
