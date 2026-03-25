package com.example.demo.exception;

/**
 * Thrown when an order cannot be placed because the requested stock is unavailable.
 */
public class InsufficientStockException extends RuntimeException {

  /**
   * Constructs the exception with the insufficient-stock message.
   *
   * @param message the message describing the stock failure
   */
  public InsufficientStockException(String message) {
    super(message);
  }
}
