import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom"; // <-- Required for useNavigate
import ProfilePage from "../ProfilePage.jsx"; // Adjust path as needed

const mockUpdateProfile = vi.fn();
const mockChangePassword = vi.fn();
const mockProfileDetails = vi.fn();
const mockUseAuth = vi.fn();
const mockNavigate = vi.fn();

vi.mock("../Auth/auth-context", () => ({
    useAuth: () => mockUseAuth(),
}));

// Mock react-router-dom to safely spy on navigation
vi.mock("react-router-dom", async (importOriginal) => {
    const actual = await importOriginal();
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

// Helper function to ALWAYS wrap ProfilePage in a MemoryRouter
const renderProfilePage = () => {
    return render(
        <MemoryRouter>
            <ProfilePage />
        </MemoryRouter>
    );
};

describe("ProfilePage", () => {
    beforeEach(() => {
        vi.resetAllMocks();

        mockUseAuth.mockReturnValue({
            user: { email: "test@example.com", displayName: "TestUser", id: 1 },
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

        // JSDOM doesn't implement window.scrollTo, so we must mock it to prevent crashes on save
        window.scrollTo = vi.fn();
    });

    // 1. Unauthenticated View
    it("shows sign in message when user is not authenticated", () => {
        mockUseAuth.mockReturnValue({ user: null });
        renderProfilePage();
        
        expect(screen.getByRole('heading', { name: "Your Profile" })).toBeInTheDocument();
        expect(screen.getByText(/Please sign in to view or change your profile/i)).toBeInTheDocument();
    });

    // 2. Navigation
    it("navigates to history page when View Order History is clicked", async () => {
        renderProfilePage();
        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeInTheDocument());

        const historyBtn = screen.getByRole("button", { name: /View Order History/i });
        await userEvent.click(historyBtn);

        expect(mockNavigate).toHaveBeenCalledWith("/history");
    });

    // 3. Tests that profile data is loaded and displayed on mount
    it("loads and displays profile data on mount", async () => {
        renderProfilePage();

        await waitFor(() => {
            expect(screen.getByDisplayValue("TestUser")).toBeInTheDocument();
            expect(screen.getByDisplayValue("test@example.com")).toBeInTheDocument();
            expect(screen.getByDisplayValue("This is my bio")).toBeInTheDocument();
        });
    });

    // 4. Tests that the balance is displayed
    it("displays the current balance", async () => {
        renderProfilePage();
        await waitFor(() => {
            expect(screen.getByText("-99.99 RPC")).toBeInTheDocument();
        });
    });

    // 5. Tests that the email field is disabled
    it("email field is disabled", async () => {
        renderProfilePage();
        await waitFor(() => {
            const emailInput = screen.getByDisplayValue("test@example.com");
            expect(emailInput.disabled).toBe(true);
        });
    });

    // 6. Tests that updateProfile is called with correct values on save
    it("calls updateProfile with displayName and bio on save", async () => {
        mockUpdateProfile.mockResolvedValue();
        renderProfilePage();

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeInTheDocument());

        const displayNameInput = screen.getByDisplayValue("TestUser");
        await userEvent.clear(displayNameInput);
        await userEvent.type(displayNameInput, "NewName");

        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            expect(mockUpdateProfile).toHaveBeenCalledWith("NewName", "This is my bio");
        });
    });

    // 7. Tests that a success message is shown after saving
    it("shows success message after successful save", async () => {
        mockUpdateProfile.mockResolvedValue();
        renderProfilePage();

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeInTheDocument());
        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            expect(screen.getByText(/Profile updated successfully/)).toBeInTheDocument();
        });
    });

    // 8. Password Update Tests
    it("calls changePassword when all password fields are provided", async () => {
        mockUpdateProfile.mockResolvedValue();
        mockChangePassword.mockResolvedValue({ message: "Password updated successfully" });
        renderProfilePage();

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("Enter your current password"), "oldPassword");
        await userEvent.type(screen.getByPlaceholderText("Enter a new password"), "newPassword123");
        await userEvent.type(screen.getByPlaceholderText("Confirm your new password"), "newPassword123");
        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            expect(mockChangePassword).toHaveBeenCalledWith("oldPassword", "newPassword123");
        });
    });

    it("shows error when password fields are incomplete", async () => {
        renderProfilePage();

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("Enter a new password"), "newPassword123");
        await userEvent.click(screen.getByText("Save Changes"));

        expect(screen.getByText("Fill out all password fields to change your password.")).toBeInTheDocument();
        expect(mockUpdateProfile).not.toHaveBeenCalled();
        expect(mockChangePassword).not.toHaveBeenCalled();
    });

    // 9. Tests that an error is shown when passwords do not match
    it("shows error when passwords do not match", async () => {
        renderProfilePage();

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("Enter your current password"), "oldPassword");
        await userEvent.type(screen.getByPlaceholderText("Enter a new password"), "password123");
        await userEvent.type(screen.getByPlaceholderText("Confirm your new password"), "differentpassword");
        await userEvent.click(screen.getByText("Save Changes"));

        expect(screen.getByText("Passwords do not match!")).toBeInTheDocument();
        expect(mockUpdateProfile).not.toHaveBeenCalled();
        expect(mockChangePassword).not.toHaveBeenCalled();
    });

    // 10. Tests that an error message is shown when updateProfile fails
    it("shows error message when updateProfile fails", async () => {
        mockUpdateProfile.mockRejectedValue(new Error("Update failed"));
        renderProfilePage();

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeInTheDocument());
        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            expect(screen.getByText("Update failed")).toBeInTheDocument();
        });
    });

    // 11. Tests that an error message is shown when changePassword fails
    it("shows error message when changePassword fails", async () => {
        mockUpdateProfile.mockResolvedValue();
        mockChangePassword.mockRejectedValue(new Error("Current password is incorrect"));
        renderProfilePage();

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("Enter your current password"), "wrongPassword");
        await userEvent.type(screen.getByPlaceholderText("Enter a new password"), "newPassword123");
        await userEvent.type(screen.getByPlaceholderText("Confirm your new password"), "newPassword123");
        await userEvent.click(screen.getByText("Save Changes"));

        await waitFor(() => {
            expect(screen.getByText("Current password is incorrect")).toBeInTheDocument();
        });
    });

    // 12. Tests that password fields are cleared after successful save
    it("clears password fields after successful save", async () => {
        mockUpdateProfile.mockResolvedValue();
        mockChangePassword.mockResolvedValue({ message: "Password updated successfully" });
        renderProfilePage();

        await waitFor(() => expect(screen.getByDisplayValue("TestUser")).toBeInTheDocument());

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