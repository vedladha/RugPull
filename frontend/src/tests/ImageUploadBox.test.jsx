import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import ImageUploadBox from "../Components/ImageUploadBox.jsx"; // Adjust path as needed

describe("ImageUploadBox", () => {
  it("renders default placeholder text initially", () => {
    render(<ImageUploadBox onImageUpload={vi.fn()} />);
    
    expect(screen.getByText("Click to upload item photo")).toBeInTheDocument();
    expect(screen.queryByAltText("Preview")).not.toBeInTheDocument();
  });

  it("handles valid image upload, shows preview, and calls callback", async () => {
    const mockOnImageUpload = vi.fn();
    const { container } = render(<ImageUploadBox onImageUpload={mockOnImageUpload} />);

    // Grab the hidden file input
    const fileInput = container.querySelector('input[type="file"]');
    
    // Create a mock image file
    const file = new File(['(⌐□_□)'], 'guitar.png', { type: 'image/png' });

    // Simulate a user uploading the file
    await userEvent.upload(fileInput, file);

    // FileReader is asynchronous, so we wait for the preview image to render
    await waitFor(() => {
      expect(screen.getByAltText("Preview")).toBeInTheDocument();
    });

    // Ensure the original placeholder text is gone
    expect(screen.queryByText("Click to upload item photo")).not.toBeInTheDocument();

    // Verify the callback was triggered with the actual file object
    expect(mockOnImageUpload).toHaveBeenCalledTimes(1);
    expect(mockOnImageUpload).toHaveBeenCalledWith(file);
  });

  it("ignores non-image files", async () => {
    const mockOnImageUpload = vi.fn();
    const { container } = render(<ImageUploadBox onImageUpload={mockOnImageUpload} />);

    const fileInput = container.querySelector('input[type="file"]');
    
    // Create a mock text file instead of an image
    const textFile = new File(['hello world'], 'notes.txt', { type: 'text/plain' });

    await userEvent.upload(fileInput, textFile);

    // Give the event loop a tiny buffer to process
    await new Promise((resolve) => setTimeout(resolve, 50));

    // The component should ignore the file and remain in its default state
    expect(screen.getByText("Click to upload item photo")).toBeInTheDocument();
    expect(screen.queryByAltText("Preview")).not.toBeInTheDocument();
    expect(mockOnImageUpload).not.toHaveBeenCalled();
  });
});