package edu.wisc.t32.exception;

/**
 * Thrown when a password change request includes an invalid new password.
 */
public class InvalidNewPasswordException extends RuntimeException {

  /**
   * Constructs the exception with the new-password validation failure message.
   *
   * @param message the message describing the validation failure
   */
  public InvalidNewPasswordException(String message) {
    super(message);
  }
}
