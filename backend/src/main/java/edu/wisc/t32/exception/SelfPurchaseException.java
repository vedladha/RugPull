package edu.wisc.t32.exception;

/**
 * Thrown when a buyer attempts to place an order for their own listing.
 */
public class SelfPurchaseException extends RuntimeException {

  /**
   * Constructs the exception with the self-purchase message.
   *
   * @param message the message describing the self-purchase failure
   */
  public SelfPurchaseException(String message) {
    super(message);
  }
}
