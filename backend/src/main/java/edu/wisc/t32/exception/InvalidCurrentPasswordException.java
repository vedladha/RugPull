package edu.wisc.t32.exception;

/**
 * Thrown when a password change request includes the wrong current password.
 */
public class InvalidCurrentPasswordException extends RuntimeException {

  /**
   * Constructs the exception with the current-password validation failure message.
   *
   * @param message the message describing the validation failure
   */
  public InvalidCurrentPasswordException(String message) {
    super(message);
  }
}
