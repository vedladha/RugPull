package edu.wisc.t32.dto;

/**
 * Data Transfer Object (DTO) for authenticated password change requests.
 */
public class PasswordChangeRequest {

  private String currentPassword;
  private String newPassword;

  /**
   * Retrieves the user's current password.
   *
   * @return the current password
   */
  public String getCurrentPassword() {
    return currentPassword;
  }

  /**
   * Sets the user's current password.
   *
   * @param currentPassword the current password to set
   */
  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  /**
   * Retrieves the user's requested new password.
   *
   * @return the new password
   */
  public String getNewPassword() {
    return newPassword;
  }

  /**
   * Sets the user's requested new password.
   *
   * @param newPassword the new password to set
   */
  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
}
