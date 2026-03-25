package edu.wisc.t32.dto;

/**
 * Data Transfer Object (DTO) for handling user profile update requests.
 * Encapsulates the data sent from the client when a user attempts to update
 * their display name or biography.
 */
public class ProfileUpdateRequest {

  /**
   * The desired new display name for the user.
   */
  private String displayName;

  /**
   * The desired new biography for the user.
   */
  private String bio;

  /**
   * Retrieves the requested display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets the requested display name.
   *
   * @param displayName the new display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Retrieves the requested biography.
   *
   * @return the biography text
   */
  public String getBio() {
    return bio;
  }

  /**
   * Sets the requested biography.
   *
   * @param bio the new biography text
   */
  public void setBio(String bio) {
    this.bio = bio;
  }
}
