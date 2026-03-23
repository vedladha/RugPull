import { render, screen, waitFor, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { AuthProvider } from "../Auth/AuthContext.jsx";
import { useAuth } from "../Auth/auth-context";

beforeEach(() => {
    global.fetch = vi.fn();
});

// Helper component to expose auth context values
const AuthConsumer = () => {
    const { user, loading } = useAuth();
    return (
        <div>
            <div data-testid="user">{user ? user.email : "no user"}</div>
            <div data-testid="loading">{loading ? "loading" : "done"}</div>
        </div>
    );
};

const renderWithAuth = (ui) => render(<AuthProvider>{ui}</AuthProvider>);

describe("AuthProvider", () => {
    // Tests that loading is true initially then false after profile fetch
    it("sets loading to false after profile fetch", async () => {
        global.fetch = vi.fn().mockResolvedValue({ ok: false });
        renderWithAuth(<AuthConsumer />);

        expect(screen.getByTestId("loading").textContent).toBe("loading");

        await waitFor(() => {
            expect(screen.getByTestId("loading").textContent).toBe("done");
        });
    });

    // Tests that user is set when profile fetch succeeds
    it("sets user when profile fetch succeeds", async () => {
        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ user: { email: "test@example.com" } }),
        });

        renderWithAuth(<AuthConsumer />);

        await waitFor(() => {
            expect(screen.getByTestId("user").textContent).toBe("test@example.com");
        });
    });

    // Tests that user is null when profile fetch fails
    it("sets user to null when profile fetch fails", async () => {
        global.fetch = vi.fn().mockResolvedValue({ ok: false });

        renderWithAuth(<AuthConsumer />);

        await waitFor(() => {
            expect(screen.getByTestId("user").textContent).toBe("no user");
        });
    });

    // Tests that user is null when profile fetch throws
    it("sets user to null when profile fetch throws", async () => {
        global.fetch = vi.fn().mockRejectedValue(new Error("Network error"));

        renderWithAuth(<AuthConsumer />);

        await waitFor(() => {
            expect(screen.getByTestId("user").textContent).toBe("no user");
        });
    });
});

describe("AuthProvider - signIn", () => {
    // Tests that signIn sets the user on success
    it("sets user after successful signIn", async () => {
        global.fetch = vi.fn()
            .mockResolvedValueOnce({ ok: false }) // initial profile fetch
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({ email: "test@example.com" }),
            });

        const SignInConsumer = () => {
            const { user, signIn } = useAuth();
            return (
                <div>
                    <div data-testid="user">{user ? user.email : "no user"}</div>
                    <button onClick={() => signIn("test@example.com", "password123")}>Sign In</button>
                </div>
            );
        };

        renderWithAuth(<SignInConsumer />);
        await waitFor(() => expect(screen.getByTestId("user").textContent).toBe("no user"));

        await act(async () => {
            screen.getByText("Sign In").click();
        });

        await waitFor(() => {
            expect(screen.getByTestId("user").textContent).toBe("test@example.com");
        });
    });

    // Tests that signIn throws on failure
    it("throws error when signIn fails", async () => {
        global.fetch = vi.fn()
            .mockResolvedValueOnce({ ok: false }) // initial profile fetch
            .mockResolvedValueOnce({ ok: false }); // login fetch

        let error = null;
        const SignInConsumer = () => {
            const { signIn } = useAuth();
            return (
                <button onClick={async () => {
                    try { await signIn("test@example.com", "wrong"); }
                    catch (e) { error = e.message; }
                }}>Sign In</button>
            );
        };

        renderWithAuth(<SignInConsumer />);
        await waitFor(() => {}); // wait for initial fetch

        await act(async () => { screen.getByText("Sign In").click(); });
        await waitFor(() => expect(error).toBe("Incorrect email or password"));
    });
});

describe("AuthProvider - signOut", () => {
    // Tests that signOut clears the user
    it("clears user after signOut", async () => {
        global.fetch = vi.fn()
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({ user: { email: "test@example.com" } }),
            })
            .mockResolvedValueOnce({ ok: true }); // logout fetch

        const SignOutConsumer = () => {
            const { user, signOut } = useAuth();
            return (
                <div>
                    <div data-testid="user">{user ? user.email : "no user"}</div>
                    <button onClick={signOut}>Sign Out</button>
                </div>
            );
        };

        renderWithAuth(<SignOutConsumer />);
        await waitFor(() => expect(screen.getByTestId("user").textContent).toBe("test@example.com"));

        await act(async () => { screen.getByText("Sign Out").click(); });
        await waitFor(() => expect(screen.getByTestId("user").textContent).toBe("no user"));
    });
});

describe("AuthProvider - register", () => {
    // Tests that register sets the user on success
    it("sets user after successful register", async () => {
        global.fetch = vi.fn()
            .mockResolvedValueOnce({ ok: false }) // initial profile fetch
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({ email: "new@example.com", displayName: "NewUser" }),
            });

        const RegisterConsumer = () => {
            const { user, register } = useAuth();
            return (
                <div>
                    <div data-testid="user">{user ? user.email : "no user"}</div>
                    <button onClick={() => register("NewUser", "new@example.com", "password123")}>Register</button>
                </div>
            );
        };

        renderWithAuth(<RegisterConsumer />);
        await waitFor(() => expect(screen.getByTestId("user").textContent).toBe("no user"));

        await act(async () => { screen.getByText("Register").click(); });
        await waitFor(() => expect(screen.getByTestId("user").textContent).toBe("new@example.com"));
    });

    // Tests that register throws on failure
    it("throws error when register fails", async () => {
        global.fetch = vi.fn()
            .mockResolvedValueOnce({ ok: false }) // initial profile fetch
            .mockResolvedValueOnce({
                ok: false,
                json: () => Promise.resolve({ message: "Email already in use" }),
            });

        let error = null;
        const RegisterConsumer = () => {
            const { register } = useAuth();
            return (
                <button onClick={async () => {
                    try { await register("User", "existing@example.com", "password"); }
                    catch (e) { error = e.message; }
                }}>Register</button>
            );
        };

        renderWithAuth(<RegisterConsumer />);
        await waitFor(() => {});

        await act(async () => { screen.getByText("Register").click(); });
        await waitFor(() => expect(error).toBe("Email already in use"));
    });
});

describe("AuthProvider - updateProfile", () => {
    // Tests that updateProfile updates the user
    it("updates user after successful updateProfile", async () => {
        global.fetch = vi.fn()
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({ user: { email: "test@example.com", displayName: "OldName" } }),
            })
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({ profile: { displayName: "NewName" } }),
            });

        const UpdateConsumer = () => {
            const { user, updateProfile } = useAuth();
            return (
                <div>
                    <div data-testid="user">{user?.displayName ?? "no user"}</div>
                    <button onClick={() => updateProfile("NewName", "New bio")}>Update</button>
                </div>
            );
        };

        renderWithAuth(<UpdateConsumer />);
        await waitFor(() => expect(screen.getByTestId("user").textContent).toBe("OldName"));

        await act(async () => { screen.getByText("Update").click(); });
        await waitFor(() => expect(screen.getByTestId("user").textContent).toBe("NewName"));
    });

    // Tests that updateProfile throws on failure
    it("throws error when updateProfile fails", async () => {
        global.fetch = vi.fn()
            .mockResolvedValueOnce({ ok: false })
            .mockResolvedValueOnce({
                ok: false,
                json: () => Promise.resolve({ error: "Update failed" }),
            });

        let error = null;
        const UpdateConsumer = () => {
            const { updateProfile } = useAuth();
            return (
                <button onClick={async () => {
                    try { await updateProfile("Name", "Bio"); }
                    catch (e) { error = e.message; }
                }}>Update</button>
            );
        };

        renderWithAuth(<UpdateConsumer />);
        await waitFor(() => {});

        await act(async () => { screen.getByText("Update").click(); });
        await waitFor(() => expect(error).toBe("Update failed"));
    });
});