import { createContext, useContext, useState, useEffect } from "react";

const AuthContext = createContext(null);
const API = "http://localhost:3001";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`${API}/api/auth/profile/`)
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => setUser(data.user ?? null))
      .finally(() => setLoading(false));
  }, []);

  async function register(displayName, email, password) {
    const response = await fetch(`${API}/api/auth/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName, email, password }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(errorData?.message || errorData?.error || "Registration failed");
    }
    const data = await response.json();
    console.log("Registered user profile:", data.user?.displayName);
    setUser(data.user);
    return data.user;
  }

  async function signIn(email, password) {
    const loginResponse = await fetch(
      `${API}/api/auth/login`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      },
    );

    if (!loginResponse.ok) throw new Error("Incorrect email or password");

    const data = await loginResponse.json();

    setUser(data.user);
    return data.user;
  }

  async function signOut() {
    await fetch(`${API}/api/auth/logout`, {
      method: "POST",
    });
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, signIn, signOut, register, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
