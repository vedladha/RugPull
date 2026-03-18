import { useState } from "react";
import "./index.css";
import { Routes, Route, useNavigate } from "react-router-dom";
import Navbar from "./Components/Navbar.jsx";
import Hero from "./Hero.jsx";
import PageCards from "./Components/PageCards.jsx";
import Listings from "./Listings.jsx";
import SellPage from "./SellPage.jsx"
import ProfilePage from "./ProfilePage.jsx"
import AuthModal from "./Auth/AuthModal.jsx";
import Footer from "./Components/Footer.jsx";

export default function App() {
  const [modal, setModal] = useState(null); // null | "signin" | "signup"
  const navigate = useNavigate();

  return (
    <>
      <Navbar onSignInClick={() => setModal("signin")} />
      <Routes>
        <Route
          path="/"
          element={
            <>
              <Hero onCreateAccountClick={() => setModal("signup")} />
              <PageCards
                onCardClick={(action) => {
                  if (action === "marketplace") {
                    navigate("/listings");
                  } else if (action === "sell") {
                    navigate("/sell")
                  }
                }}
              />
            </>
          }
        />
        <Route path="/listings" element={<Listings />} />
        <Route path="/sell" element={<SellPage />} />
        <Route path="/listings" element={
          <Listings />
        }
        />
        <Route path="/profile" element={
          <ProfilePage />
        } />
      </Routes>
      <Footer />
      {modal && (
        <AuthModal
          initialSignUp={modal === "signup"}
          onClose={() => setModal(null)}
        />
      )}
    </>
  );
}
