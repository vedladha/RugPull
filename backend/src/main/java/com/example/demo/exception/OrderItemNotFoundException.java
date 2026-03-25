package com.example.demo.exception;

/**
 * Thrown when an order references an item that does not exist or is unavailable.
 */
public class OrderItemNotFoundException extends RuntimeException {

  /**
   * Constructs the exception with the missing-item message.
   *
   * @param message the message describing the missing-item failure
   */
  public OrderItemNotFoundException(String message) {
    super(message);
  }
}
