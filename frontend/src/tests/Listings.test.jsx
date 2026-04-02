import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import Listings from "../Listings.jsx";

const mockListings = [
    { id: 1, name: "Guitar", description: "Great condition", price: 5.2, sellerName: "john" },
    { id: 2, name: "Bike", description: "Barely used", price: 10.0, sellerName: "jane" },
];

beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
});

describe("Listings", () => {
    // Tests that loading state is shown initially
    it("shows loading state initially", async () => {
        let resolve;
        vi.stubGlobal("fetch", vi.fn(() => new Promise((r) => { resolve = r; }))); // never resolves
        render(<Listings />);
        expect(screen.getByText("Loading listings...")).toBeInTheDocument();

        resolve({ ok: true, json: () => Promise.resolve({ items: [] })})
        await waitFor(() => expect(screen.queryByText("Loading listings...")).toBeNull());
    });

    // Tests that listings are displayed after fetch
    it("displays listings after fetch", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        render(<Listings />);

        await waitFor(() => {
            expect(screen.getByText("Guitar")).toBeInTheDocument();
            expect(screen.getByText("Bike")).toBeInTheDocument();
        });

    });

    // Tests that error state is shown when fetch fails
    it("shows error state when fetch fails", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: false,
            status: 500,
        }));

        render(<Listings />);

        await waitFor(() => {
            expect(screen.getByText(/Error loading listings/)).toBeInTheDocument();
        });
    });

    // Tests that no listings message is shown when list is empty
    it("shows no listings message when list is empty", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: [] }),
        }));

        render(<Listings />);

        await waitFor(() => {
            expect(screen.getByText("No listings found matching your filters.")).toBeInTheDocument();
        });
    });

    // Tests that min price filter works
    it("filters listings by min price", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        render(<Listings />);
        await waitFor(() => expect(screen.getByText("Guitar")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("0"), "8");

        await waitFor(() => {
            expect(screen.queryByText("Guitar")).toBeNull();
            expect(screen.getByText("Bike")).toBeInTheDocument();
        });
    });

    // Tests that max price filter works
    it("filters listings by max price", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        render(<Listings />);
        await waitFor(() => expect(screen.getByText("Guitar")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("No limit"), "7");

        await waitFor(() => {
            expect(screen.getByText("Guitar")).toBeInTheDocument();
            expect(screen.queryByText("Bike")).toBeNull();
        });
    });

    // Tests that keyword filter works
    it("filters listings by keyword", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        render(<Listings />);
        await waitFor(() => expect(screen.getByText("Guitar")).toBeInTheDocument());

        await userEvent.type(screen.getByPlaceholderText("Search listings..."), "Guitar");

        await waitFor(() => {
            expect(screen.getByText("Guitar")).toBeInTheDocument();
            expect(screen.queryByText("Bike")).toBeNull();
        });
    });

    it("opens and closes a listing details modal when a listing is clicked", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({ items: mockListings }),
        }));

        render(<Listings />);
        await waitFor(() => expect(screen.getByText("Guitar")).toBeInTheDocument());

        await userEvent.click(screen.getByRole("button", { name: /view details for guitar/i }));

        expect(screen.getByRole("dialog", { name: "Guitar" })).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Buy" })).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Add to Cart" })).toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", { name: /close listing details/i }));

        await waitFor(() => {
            expect(screen.queryByRole("dialog")).toBeNull();
        });
    });
});
