package edu.wisc.t32.exception;

/**
 * Thrown when an order payment fails due to wallet configuration or transfer-processing issues.
 */
public class OrderPaymentException extends RuntimeException {

  /**
   * Constructs the exception with the payment-failure message.
   *
   * @param message the message describing the payment failure
   */
  public OrderPaymentException(String message) {
    super(message);
  }

  /**
   * Constructs the exception with the payment-failure message and cause.
   *
   * @param message the message describing the payment failure
   * @param cause the underlying cause of the failure
   */
  public OrderPaymentException(String message, Throwable cause) {
    super(message, cause);
  }
}
