import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import ProfilePage from "../ProfilePage.jsx";

const mockUpdateProfile = vi.fn();
const mockChangePassword = vi.fn();
const mockProfileDetails = vi.fn();
const mockUseAuth = vi.fn();

vi.mock("../Auth/auth-context", () => ({
    useAuth: () => mockUseAuth(),
}));

describe("ProfilePage", () => {
    beforeEach(() => {
        mockUseAuth.mockReturnValue({
            user: { email: "test@example.com" },
            userBalance: -99.99,
            updateUserBalance: vi.fn(),
            profileDetails: mockProfileDetails,
            updateProfile: mockUpdateProfile,
            changePassword: mockChangePassword,
        });

        mockProfileDetails.mockResolvedValue({
            profile: {
                displayName: "TestUser",
                bio: "This is my bio",
            },
        });

        mockUpdateProfile.mockReset();
        mockChangePassword.mockReset();
    });

    // Tests that profile data is loaded and displayed on mount
    it("loads and displays profile data on mount", async () => {
        render(<ProfilePage />);

        await waitFor(() => {
            expect(screen.getByDisplayValue("TestUser")).toBeDefined();
            expect(screen.getByDisplayValue("test@example.com")).toBeDefined();
            expect(screen.getByDisplayValue("This is my bio")).toBeDefined();
        });
    });

    // Tests that the balance is displayed
    it("displays the current balance", async () => {
        render(<ProfilePage />);
        await waitFor(() => {
            expect(screen.getByText(/-99\.99/)).toBeDefined();
        });
    });

    // Tests that the email field is disabled
    it("email field is disabled", async () => {
        render(<ProfilePage />);
        await waitFor(() => {
            const emailInput = screen.getByDisplayValue("test@example.com");
            expect(emailInput.disabled).toBe(true);
        });
    });

    // Tests that updateProfile is called with correct values on save
    it("calls updateProfile with displayName and bio on save", async () => {
        mockUpdateProfile.mockResolvedValue();
        render(<ProfilePage />);

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeDefined());

        const displayNameInput = screen.getByDisplayValue("TestUser");
        await userEvent.clear(displayNameInput);
        await userEvent.type(displayNameInput, "NewName");

        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            expect(mockUpdateProfile).toHaveBeenCalledWith("NewName", "This is my bio");
        });
    });

    // Tests that a success message is shown after saving
    it("shows success message after successful save", async () => {
        mockUpdateProfile.mockResolvedValue();
        render(<ProfilePage />);

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeDefined());
        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            expect(screen.getByText(/Profile updated successfully/)).toBeDefined();
        });
    });

    it("calls changePassword when all password fields are provided", async () => {
        mockUpdateProfile.mockResolvedValue();
        mockChangePassword.mockResolvedValue({ message: "Password updated successfully" });
        render(<ProfilePage />);

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeDefined());

        await userEvent.type(screen.getByPlaceholderText("Enter your current password"), "oldPassword");
        await userEvent.type(screen.getByPlaceholderText("Enter a new password"), "newPassword123");
        await userEvent.type(screen.getByPlaceholderText("Confirm your new password"), "newPassword123");
        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            expect(mockChangePassword).toHaveBeenCalledWith("oldPassword", "newPassword123");
        });
    });

    it("shows error when password fields are incomplete", async () => {
        render(<ProfilePage />);

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeDefined());

        await userEvent.type(screen.getByPlaceholderText("Enter a new password"), "newPassword123");
        await userEvent.click(screen.getByText("Save Changes"));

        expect(screen.getByText("Fill out all password fields to change your password.")).toBeDefined();
        expect(mockUpdateProfile).not.toHaveBeenCalled();
        expect(mockChangePassword).not.toHaveBeenCalled();
    });

    // Tests that an error is shown when passwords do not match
    it("shows error when passwords do not match", async () => {
        render(<ProfilePage />);

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeDefined());

        await userEvent.type(screen.getByPlaceholderText("Enter your current password"), "oldPassword");
        await userEvent.type(screen.getByPlaceholderText("Enter a new password"), "password123");
        await userEvent.type(screen.getByPlaceholderText("Confirm your new password"), "differentpassword");
        await userEvent.click(screen.getByText("Save Changes"));

        expect(screen.getByText("Passwords do not match!")).toBeDefined();
        expect(mockUpdateProfile).not.toHaveBeenCalled();
        expect(mockChangePassword).not.toHaveBeenCalled();
    });

    // Tests that an error message is shown when updateProfile fails
    it("shows error message when updateProfile fails", async () => {
        mockUpdateProfile.mockRejectedValue(new Error("Update failed"));
        render(<ProfilePage />);

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeDefined());
        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            expect(screen.getByText("Update failed")).toBeDefined();
        });
    });

    it("shows error message when changePassword fails", async () => {
        mockUpdateProfile.mockResolvedValue();
        mockChangePassword.mockRejectedValue(new Error("Current password is incorrect"));
        render(<ProfilePage />);

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeDefined());

        await userEvent.type(screen.getByPlaceholderText("Enter your current password"), "wrongPassword");
        await userEvent.type(screen.getByPlaceholderText("Enter a new password"), "newPassword123");
        await userEvent.type(screen.getByPlaceholderText("Confirm your new password"), "newPassword123");
        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            expect(screen.getByText("Current password is incorrect")).toBeDefined();
        });
    });

    // Tests that password fields are cleared after successful save
    it("clears password fields after successful save", async () => {
        mockUpdateProfile.mockResolvedValue();
        mockChangePassword.mockResolvedValue({ message: "Password updated successfully" });
        render(<ProfilePage />);

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeDefined());

        await userEvent.type(screen.getByPlaceholderText("Enter your current password"), "oldPassword");
        await userEvent.type(screen.getByPlaceholderText("Enter a new password"), "password123");
        await userEvent.type(screen.getByPlaceholderText("Confirm your new password"), "password123");
        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            expect(screen.getByPlaceholderText("Enter your current password").value).toBe("");
            expect(screen.getByPlaceholderText("Enter a new password").value).toBe("");
            expect(screen.getByPlaceholderText("Confirm your new password").value).toBe("");
        });
    });
});
