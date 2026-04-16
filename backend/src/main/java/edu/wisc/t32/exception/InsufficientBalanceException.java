package edu.wisc.t32.exception;

/**
 * Thrown when a checkout payment cannot be completed because the buyer lacks sufficient RPC.
 */
public class InsufficientBalanceException extends RuntimeException {

  /**
   * Constructs the exception with the insufficient-balance message.
   *
   * @param message the message describing the balance failure
   */
  public InsufficientBalanceException(String message) {
    super(message);
  }
}
