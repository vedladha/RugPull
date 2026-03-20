package com.example.demo.services;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles image uploads.
 * Has security validation, unique filename generation, and physical
 * storage of the image file in the images directory.
 */
@Service
public class FileService {

  /**
   * The directory where images are stored.
   * Linked to a Docker volume with docker-compose.yml.
   */
  @Value("${UPLOAD_PATH:images}")
  private String uploadPath;

  private Path root;

  /**
   * Initialization method for the FileService used by springboot and tests.
   */
  @PostConstruct
  public void init() {
    this.root = Paths.get(uploadPath);
  }

  /**
   * Performs security checks on an uploaded image file.
   * Checks include:
   * - File is not empty
   * - File size is no larger than 5 MB
   * - MIME type starts with "image/"
   * - Magic Byte check ensures the file is a renderable image
   *
   * @param file The MultipartFile to validate received from the Controller.
   * @throws RuntimeException if validation fails.
   */
  public void validate(MultipartFile file) {
    if (file.isEmpty()) {
      throw new RuntimeException("Failed to store empty file.");
    }

    // Size Check (5MB)
    if (file.getSize() > 5 * 1024 * 1024) {
      throw new RuntimeException("File is too large! Max 5MB allowed.");
    }

    // Extension/MIME Check
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new RuntimeException("Only image files (JPG, PNG, etc.) are allowed.");
    }

    // Magic Byte Check (Ensures it's actually an image, not a renamed .exe)
    try (InputStream is = file.getInputStream()) {
      if (ImageIO.read(is) == null) {
        throw new RuntimeException("The file is not a valid image.");
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not read file for validation.");
    }
  }

  /**
   * Persists the file to the local filesystem with a unique name.
   *
   * @param file The validated MultipartFile to be saved.
   * @return A relative URL string that can be used to access the file.
   * @throws RuntimeException if the directory cannot be created or the file cannot be written.
   */
  public String save(MultipartFile file) {
    try {
      // Ensure the directory exists
      if (!Files.exists(root)) {
        Files.createDirectories(root);
      }

      // Generate a random unique name
      String extension = getFileExtension(file.getOriginalFilename());
      String filename = UUID.randomUUID().toString() + extension;

      // Copy the file to the folder
      Files.copy(file.getInputStream(), this.root.resolve(filename),
          StandardCopyOption.REPLACE_EXISTING);

      return "/images/" + filename;

    } catch (Exception e) {
      throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
    }
  }

  /**
   * Extracts the file extension from a filename.
   *
   * @param filename The filename.
   * @return The extension including the dot, defaults to ".jpg".
   */
  private String getFileExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
      return ".jpg";
    }
    return filename.substring(filename.lastIndexOf("."));
  }
}
