package com.example.demo.exception;

/**
 * Thrown when a registration request uses an email address that already exists.
 */
public class DuplicateEmailException extends RuntimeException {

  /**
   * Constructs the exception with the duplicate email message.
   *
   * @param message the message describing the duplicate email failure
   */
  public DuplicateEmailException(String message) {
    super(message);
  }
}
