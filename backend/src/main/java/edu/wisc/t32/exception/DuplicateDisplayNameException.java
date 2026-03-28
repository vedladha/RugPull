package edu.wisc.t32.exception;

/**
 * Thrown when a registration request uses a display name that is already taken.
 */
public class DuplicateDisplayNameException extends RuntimeException {

  /**
   * Constructs the exception with the duplicate display-name message.
   *
   * @param message the message describing the duplicate display-name failure
   */
  public DuplicateDisplayNameException(String message) {
    super(message);
  }
}
