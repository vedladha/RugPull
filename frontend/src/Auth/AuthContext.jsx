import { useState, useEffect } from "react";
import { AuthContext } from "./auth-context";

const API = "http://localhost:3001";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`${API}/auth/profile`, {
      credentials: "include",
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => setUser(data?.user ?? null))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  async function register(displayName, email, password) {
    const response = await fetch(`${API}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName, email, password }),
      credentials: "include",
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || "Registration failed");
    }
    const profile = await response.json();
    console.log("Registered user profile:", profile.displayName);
    setUser(profile);
    return profile;
  }

  async function signIn(email, password) {
    const loginResponse = await fetch(
      `${API}/auth/login`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
        credentials: "include",
      },
    );

    if (!loginResponse.ok) throw new Error("Incorrect email or password");

    /*const profileResponse = await fetch(`${API}/auth/profile`, {
      credentials: "include",

    });*/
    const data = await loginResponse.json();

    setUser(data);
    return data;
  }

async function walletBalance() {
    const response = await fetch(
      `${API}/wallets`,
      {
        method: "GET",
        credentials: "include"
      });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || "Failed to fetch balance");
    }

    const balance = await response.text();
    return Number(balance);
  }

  async function signOut() {
    await fetch(`${API}/auth/logout`, {
      method: "POST",
      credentials: "include",
    });
    setUser(null);
  }

  async function profileDetails() {
    const response = await fetch(`${API}/profile/me`, {
      method: "GET",
      credentials: "include",
    });


    if (!response.ok) {
      throw new Error("Could not retrieve user data");
    }

    const data = await response.json();
    return data;
  }

  async function updateProfile(displayName, bio) {
    const payload = {};
    if (displayName) payload.displayName = displayName;
    if (bio) payload.bio = bio;

    const response = await fetch(`${API}/profile/me`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
      credentials: "include",
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || "Failed to update profile");
    }

    const data = await response.json();

    setUser((prevUser) => ({
      ...prevUser,
      displayName: data.profile.displayName,
      userProfile: data.profile
    }));

    return data.profile;
  }

  return (
    <AuthContext.Provider value={{ user, signIn, signOut, walletBalance, profileDetails, updateProfile, register, loading }}>
      {children}
    </AuthContext.Provider>
  );
}
