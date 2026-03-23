import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import AuthModal from "../Auth/AuthModal.jsx";

const mockSignIn = vi.fn();
const mockRegister = vi.fn();
const mockUseAuth = vi.fn();

vi.mock("../Auth/auth-context", () => ({
    useAuth: () => mockUseAuth(),
}));

describe("AuthModal", () => {
    beforeEach(() => {
        mockUseAuth.mockReturnValue({ signIn: mockSignIn, register: mockRegister });
        mockSignIn.mockReset();
        mockRegister.mockReset();
    });

    // Tests that the sign in form is shown by default
    it("renders sign in form by default", () => {
        render(<AuthModal onClose={vi.fn()} />);
        expect(screen.getByText("Welcome back")).toBeDefined();
        expect(screen.getByPlaceholderText("you@example.com")).toBeDefined();
        expect(screen.getByPlaceholderText("••••••••")).toBeDefined();
    });

    // Tests that the sign up form is shown when initialSignUp is true
    it("renders sign up form when initialSignUp is true", () => {
        render(<AuthModal initialSignUp={true} onClose={vi.fn()} />);
        expect(screen.getByText("Create account")).toBeDefined();
        expect(screen.getByPlaceholderText("display name")).toBeDefined();
    });

    // Tests that clicking "Create account" switches to sign up form
    it("switches to sign up form when Create account is clicked", async () => {
        render(<AuthModal onClose={vi.fn()} />);
        await userEvent.click(screen.getByText("Create account"));
        expect(screen.getByPlaceholderText("display name")).toBeDefined();
    });

    // Tests that clicking "Sign in" switches back to sign in form
    it("switches to sign in form when Sign in is clicked", async () => {
        render(<AuthModal initialSignUp={true} onClose={vi.fn()} />);
        await userEvent.click(screen.getByText("Sign in"));
        expect(screen.queryByPlaceholderText("display name")).toBeNull();
    });

    // Tests that onClose is called when the close button is clicked
    it("calls onClose when close button is clicked", async () => {
        const onClose = vi.fn();
        render(<AuthModal onClose={onClose} />);
        await userEvent.click(screen.getByText("✕"));
        expect(onClose).toHaveBeenCalled();
    });

    // Tests that onClose is called when the overlay is clicked
    it("calls onClose when overlay is clicked", async () => {
        const onClose = vi.fn();
        render(<AuthModal onClose={onClose} />);
        await userEvent.click(document.querySelector(".modal-overlay"));
        expect(onClose).toHaveBeenCalled();
    });

    // Tests that clicking inside the modal does not close it
    it("does not call onClose when modal content is clicked", async () => {
        const onClose = vi.fn();
        render(<AuthModal onClose={onClose} />);
        await userEvent.click(document.querySelector(".modal"));
        expect(onClose).not.toHaveBeenCalled();
    });

    // Tests that signIn is called with the correct credentials
    it("calls signIn with email and password on submit", async () => {
        mockSignIn.mockResolvedValue();
        const onClose = vi.fn();
        render(<AuthModal onClose={onClose} />);

        await userEvent.type(screen.getByPlaceholderText("you@example.com"), "test@example.com");
        await userEvent.type(screen.getByPlaceholderText("••••••••"), "password123");
        await userEvent.click(screen.getByText("Sign In"));

        expect(mockSignIn).toHaveBeenCalledWith("test@example.com", "password123");
        await waitFor(() => expect(onClose).toHaveBeenCalled());
    });

    // Tests that register is called with the correct credentials
    it("calls register with displayName, email and password on submit", async () => {
        mockRegister.mockResolvedValue();
        const onClose = vi.fn();
        render(<AuthModal initialSignUp={true} onClose={onClose} />);

        await userEvent.type(screen.getByPlaceholderText("display name"), "TestUser");
        await userEvent.type(screen.getByPlaceholderText("you@example.com"), "test@example.com");
        await userEvent.type(screen.getByPlaceholderText("••••••••"), "password123");
        await userEvent.click(screen.getByText("Create Account"));

        expect(mockRegister).toHaveBeenCalledWith("TestUser", "test@example.com", "password123");
        await waitFor(() => expect(onClose).toHaveBeenCalled());
    });

    // Tests that an error message is shown when signIn fails
    it("displays error message when signIn fails", async () => {
        mockSignIn.mockRejectedValue(new Error("Invalid credentials"));
        render(<AuthModal onClose={vi.fn()} />);

        await userEvent.type(screen.getByPlaceholderText("you@example.com"), "test@example.com");
        await userEvent.type(screen.getByPlaceholderText("••••••••"), "wrongpassword");
        await userEvent.click(screen.getByText("Sign In"));

        await waitFor(() => expect(screen.getByText("Invalid credentials")).toBeDefined());
    });

    // Tests that an error message is shown when register fails
    it("displays error message when register fails", async () => {
        mockRegister.mockRejectedValue(new Error("Email already in use"));
        render(<AuthModal initialSignUp={true} onClose={vi.fn()} />);

        await userEvent.type(screen.getByPlaceholderText("display name"), "TestUser");
        await userEvent.type(screen.getByPlaceholderText("you@example.com"), "test@example.com");
        await userEvent.type(screen.getByPlaceholderText("••••••••"), "password123");
        await userEvent.click(screen.getByText("Create Account"));

        await waitFor(() => expect(screen.getByText("Email already in use")).toBeDefined());
    });
});