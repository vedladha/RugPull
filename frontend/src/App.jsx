import "./index.css";
import { Routes, Route } from "react-router-dom";
import Navbar from "./Components/Navbar.jsx";
import History from "./History.jsx";
import Listings from "./Listings.jsx";
import WishlistPage from "./WishlistPage.jsx";
import SellPage from "./Pages/SellPage.jsx";
import ProfilePage from "./ProfilePage.jsx";
import Footer from "./Components/Footer.jsx";
import AuthPage from "./Pages/AuthPage.jsx";
import CartPage from "./Pages/CartPage.jsx";
import OrderPage from "./Pages/OrderPage.jsx";
import EarnPage from "./Pages/EarnPage.jsx";
import FrontPage from "./Pages/FrontPage.jsx"

export default function App() {

  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<FrontPage />} />
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
        <Route path="/profile" element={
          <ProfilePage />
        } />
        <Route path="/wishlist" element={
          <WishlistPage />
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
        <Route path="/earn" element={
          <EarnPage />
        } />
        <Route path="/history" element={
          <History />
        } />
      </Routes>
      <Footer />
    </>
  );
}
