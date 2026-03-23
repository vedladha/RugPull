import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter, useLocation } from "react-router-dom";
import Navbar from "../Components/Navbar.jsx";

const mockUseAuth = vi.fn();

vi.mock("../Auth/auth-context.js", () => ({
  useAuth: () => mockUseAuth(),
}));

const LocationDisplay = () => {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}</div>;
};

describe("Navbar", () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue({ user: null, signOut: vi.fn() });
  });

  it("calls onSignInClick when Sign In button is clicked", async () => {
    const onSignInClick = vi.fn();
    render(
      <MemoryRouter initialEntries={["/"]}>
        <Navbar onSignInClick={onSignInClick}/>
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("Sign In"));
    expect(onSignInClick).toHaveBeenCalled();
  });

  it("calls signOut when Sign Out button is clicked", async () => {
    const signOut = vi.fn();
    mockUseAuth.mockReturnValue({ user: { displayName: "Test User" }, signOut });

    render(
      <MemoryRouter initialEntries={["/"]}>
        <Navbar />
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("Sign Out"));
    expect(signOut).toHaveBeenCalled();
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
    mockUseAuth.mockReturnValue({ user: {displayName: "Test User" }, signOut: vi.fn() });
    render(
      <MemoryRouter initialEntries={["/"]}>
        <Navbar />
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("Hello, Test User!"));
    expect(screen.getByTestId("location").textContent).toBe("/profile");
  });

  /*it("navigates to /sell when Sell is clicked", async () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <Navbar />
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("Sell"));
    expect(screen.getByTestId("location").textContent).toBe("/sell");
  });*/

  it("calls onSignInClick when Sign In button is clicked", async () => {
    const onSignInClick = vi.fn();
    render(
      <MemoryRouter initialEntries={["/"]}>
        <Navbar onSignInClick={onSignInClick} />
        <LocationDisplay />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByRole("button", { name: "Sign In" }));
    expect(onSignInClick).toHaveBeenCalled();
  });
});
