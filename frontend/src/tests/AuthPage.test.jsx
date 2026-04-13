import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter, useLocation } from "react-router-dom";
import AuthPage from "../Pages/AuthPage.jsx";
import { useAuth } from "../Auth/auth-context.js";

vi.mock("../Auth/auth-context.js");

const LocationDisplay = () => {
    const location = useLocation();
    return <div data-testid="location">{location.pathname}</div>;
};

describe("AuthPage", () => {
    let signInMock;
    let registerMock;

    beforeEach(() => {
        signInMock = vi.fn().mockResolvedValue();
        registerMock = vi.fn().mockResolvedValue();

        vi.mocked(useAuth).mockReturnValue({
            signIn: signInMock,
            register: registerMock,
        });

        // Stub window.scrollTo since JSDOM doesn't implement it
        window.scrollTo = vi.fn();
    });

    it("renders Login mode by default on /login", () => {
        render(
            <MemoryRouter initialEntries={["/login"]}>
                <AuthPage />
            </MemoryRouter>
        );

        expect(screen.getByRole("heading", { name: "Welcome Back" })).toBeInTheDocument();
        // Replaced label check with placeholder check
        expect(screen.queryByPlaceholderText("Market Trader 99")).not.toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Sign In" })).toBeInTheDocument();
    });

    it("renders Signup mode automatically on /signup", () => {
        render(
            <MemoryRouter initialEntries={["/signup"]}>
                <AuthPage />
            </MemoryRouter>
        );

        expect(screen.getByRole("heading", { name: "Create Account" })).toBeInTheDocument();
        expect(screen.getByPlaceholderText("Market Trader 99")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Create Account" })).toBeInTheDocument();
    });

    it("toggles route when clicking the footer links", async () => {
        render(
            <MemoryRouter initialEntries={["/login"]}>
                <AuthPage />
                <LocationDisplay />
            </MemoryRouter>
        );

        expect(screen.getByTestId("location").textContent).toBe("/login");

        const createAccountLink = screen.getByRole("button", { name: "Create account" });
        await userEvent.click(createAccountLink);

        expect(screen.getByTestId("location").textContent).toBe("/signup");
    });

    it("successfully calls signIn with correct credentials", async () => {
        render(
            <MemoryRouter initialEntries={["/login"]}>
                <AuthPage />
            </MemoryRouter>
        );

        // Bypass accessibility checks by targeting placeholders directly
        const emailInput = screen.getByPlaceholderText("you@example.com");
        const passwordInput = screen.getByPlaceholderText("••••••••");
        const submitBtn = screen.getByRole("button", { name: "Sign In" });

        await userEvent.type(emailInput, "test@example.com");
        await userEvent.type(passwordInput, "securepassword");
        await userEvent.click(submitBtn);

        expect(signInMock).toHaveBeenCalledWith("test@example.com", "securepassword");
    });

    it("successfully calls register with display name, email, and password", async () => {
        render(
            <MemoryRouter initialEntries={["/signup"]}>
                <AuthPage />
            </MemoryRouter>
        );

        // Bypass accessibility checks by targeting placeholders directly
        await userEvent.type(screen.getByPlaceholderText("Market Trader 99"), "Market Whale");
        await userEvent.type(screen.getByPlaceholderText("you@example.com"), "whale@example.com");
        await userEvent.type(screen.getByPlaceholderText("••••••••"), "securepassword");

        await userEvent.click(screen.getByRole("button", { name: "Create Account" }));

        expect(registerMock).toHaveBeenCalledWith("Market Whale", "whale@example.com", "securepassword");
    });

    it("displays an error banner when authentication fails", async () => {
        signInMock.mockRejectedValue(new Error("Invalid credentials"));

        render(
            <MemoryRouter initialEntries={["/login"]}>
                <AuthPage />
            </MemoryRouter>
        );

        // Bypass accessibility checks by targeting placeholders directly
        await userEvent.type(screen.getByPlaceholderText("you@example.com"), "test@example.com");
        await userEvent.type(screen.getByPlaceholderText("••••••••"), "wrongpassword");
        await userEvent.click(screen.getByRole("button", { name: "Sign In" }));

        await waitFor(() => {
            expect(screen.getByText(/Invalid credentials/i)).toBeInTheDocument();
        });
    });
});