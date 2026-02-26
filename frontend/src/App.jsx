import { useState } from "react";
import "./index.css";
import Navbar from "./Components/Navbar.jsx";
import Hero from "./Hero.jsx";
import PageCards from "./Components/PageCards.jsx";
import AuthModal from "./Auth/AuthModal.jsx";
import Footer from "./Components/Footer.jsx";

export default function App() {
  const [modal, setModal] = useState(null); // null | "signin" | "signup"

  return (
    <>
      <Navbar onSignInClick={() => setModal("signin")} />

      <Hero onCreateAccountClick={() => setModal("signup")} />

      <PageCards />

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
