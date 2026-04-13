import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter, useLocation } from "react-router-dom";
import Navbar from "../Components/Navbar.jsx";
import { useAuth } from "../Auth/auth-context.js";

vi.mock("../Auth/auth-context.js");

const LocationDisplay = () => {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}</div>;
};

describe("Navbar", () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReturnValue({ 
        user: null, 
        signOut: vi.fn(), 
        walletBalance: vi.fn().mockResolvedValue(-999.99) 
    });
  });

  it("navigates to /login when Sign In button is clicked", async () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <Navbar />
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("Sign In"));
    // We expect the router to change the URL, not a prop function to fire
    expect(screen.getByTestId("location").textContent).toBe("/login");
  });

  it("calls signOut when Sign Out button is clicked", async () => {
    const signOutMock = vi.fn();
    vi.mocked(useAuth).mockReturnValue({ 
        user: { displayName: "Test User" }, 
        signOut: signOutMock, 
        walletBalance: vi.fn().mockResolvedValue(-999.99) 
    });

    render(
      <MemoryRouter initialEntries={["/"]}>
        <Navbar />
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("Sign Out"));
    expect(signOutMock).toHaveBeenCalled();
  });

  it("navigates to / when logo is clicked", async () => {
    render(
      <MemoryRouter initialEntries={["/listings"]}>
        <Navbar />
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("$RPC Market"));
    expect(screen.getByTestId("location").textContent).toBe("/");
  });

  it("navigates to /listings when Marketplace is clicked", async () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <Navbar />
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("Marketplace"));
    expect(screen.getByTestId("location").textContent).toBe("/listings");
  });

  it("navigates to /profile when user name is clicked", async () => {
    vi.mocked(useAuth).mockReturnValue({ 
        user: {displayName: "Test User" }, 
        signOut: vi.fn(), 
        walletBalance: vi.fn().mockResolvedValue(-999.99) 
    });
    
    render(
      <MemoryRouter initialEntries={["/"]}>
        <Navbar />
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("Hello, Test User!"));
    expect(screen.getByTestId("location").textContent).toBe("/profile");
  });

  it("navigates to /wishlist when Wishlist is clicked", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { displayName: "Test User" },
      signOut: vi.fn(),
      walletBalance: vi.fn().mockResolvedValue(-999.99),
    });

    render(
      <MemoryRouter initialEntries={["/"]}>
        <Navbar />
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("Wishlist"));
    expect(screen.getByTestId("location").textContent).toBe("/wishlist");
  });
  it("navigates to /sell when Sell is clicked", async () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <Navbar />
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("Sell"));
    expect(screen.getByTestId("location").textContent).toBe("/sell");
  });
});
