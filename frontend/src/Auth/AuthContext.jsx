import { createContext, useContext, useState, useEffect } from "react";

const AuthContext = createContext(null);
const API = "http://localhost:3001";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`${API}/auth/profile/`, {
      credentials: "include",
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => setUser(data.user ?? null))
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

  async function signOut() {
    await fetch(`${API}/auth/logout`, {
      method: "POST",
      credentials: "include",
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
