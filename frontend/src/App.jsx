import { useState } from "react";
import "./index.css";
import Navbar from "./Components/Navbar.jsx";
import Hero from "./Hero.jsx";
import PageCards from "./Components/PageCards.jsx";
import Listings from "./Listings.jsx";
import AuthModal from "./Auth/AuthModal.jsx";
import Footer from "./Components/Footer.jsx";

export default function App() {
  const [currentPage, setCurrentPage] = useState("home"); // "home" | "listings"
  const [modal, setModal] = useState(null); // null | "signin" | "signup"

  return (
    <>
      <Navbar
        onSignInClick={() => setModal("signin")}
        onNavigate={setCurrentPage}
        currentPage={currentPage}
      />

      {currentPage === "home" ? (
        <>
          <Hero onCreateAccountClick={() => setModal("signup")} />
          <PageCards
            onCardClick={(action) => {
              if (action === "marketplace") {
                setCurrentPage("listings");
              }

            }}
          />
        </>
      ) : currentPage === "listings" ? (
        <Listings />
      ) : null}

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
