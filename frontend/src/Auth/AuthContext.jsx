import { createContext, useContext, useState, useEffect } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetch("http://localhost:8080/auth/me", {
            credentials: "include",
        })
            .then((res) => res.ok ? res.json() : null)
            .then((data) => setUser(data.user ?? null))
            .finally(() => setLoading(false));
    }, []);

    async function register(username, email, password) {
        const response = await fetch("http://localhost:8080/auth/register", {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, email, password }),
        });

        if (!response.ok) throw new Error("Error registering user");
        const data = await response.json();
        return data;
    }

    async function signIn(email, password) {
        const response = await fetch("http://localhost:8080/auth/login", {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password }),
        });

        if (!response.ok) throw new Error("Invalid credentials");

        const data = await response.json();
        setUser(data.user);
        return data;
    }

    async function signOut() {
        await fetch("http://localhost:8080/auth/logout", {
            method: "POST",
            credentials: "include",
        });
        setUser(null);
    }

    return (
        <AuthContext.Provider value={{ user, signIn, signOut, loading }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}