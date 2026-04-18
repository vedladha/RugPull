package edu.wisc.t32.controller;

import edu.wisc.t32.exception.InsufficientBalanceException;
import edu.wisc.t32.exception.InsufficientStockException;
import edu.wisc.t32.exception.OrderItemNotFoundException;
import edu.wisc.t32.exception.OrderPaymentException;
import edu.wisc.t32.exception.SelfPurchaseException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles order-related exceptions for {@link OrderController}.
 */
@RestControllerAdvice(assignableTypes = OrderController.class)
public class OrderExceptionHandler {

  /**
   * Returns the existing 404 response body when the requested item is missing.
   *
   * @param exception the missing-item exception raised during purchase
   * @return a not found response with the original item error message
   */
  @ExceptionHandler(OrderItemNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleMissingItem(
      OrderItemNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", exception.getMessage()));
  }

  /**
   * Returns the existing 400 response body when stock is no longer available.
   *
   * @param exception the insufficient-stock exception raised during purchase
   * @return a bad request response with the original stock error message
   */
  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<Map<String, String>> handleInsufficientStock(
      InsufficientStockException exception) {
    return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
  }

  /**
   * Returns a bad-request response when the buyer cannot cover the RPC total.
   *
   * @param exception the insufficient-balance exception raised during checkout
   * @return a bad request response with the original payment error message
   */
  @ExceptionHandler(InsufficientBalanceException.class)
  public ResponseEntity<Map<String, String>> handleInsufficientBalance(
      InsufficientBalanceException exception) {
    return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
  }

  /**
   * Returns a bad-request response when the user attempts to buy their own listing.
   *
   * @param exception the self-purchase exception raised during checkout
   * @return a bad request response with the original payment error message
   */
  @ExceptionHandler(SelfPurchaseException.class)
  public ResponseEntity<Map<String, String>> handleSelfPurchase(
      SelfPurchaseException exception) {
    return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
  }

  /**
   * Returns an internal-server-error response when checkout payment processing fails.
   *
   * @param exception the payment exception raised during checkout
   * @return an internal server error response with the original payment error message
   */
  @ExceptionHandler(OrderPaymentException.class)
  public ResponseEntity<Map<String, String>> handlePaymentFailure(
      OrderPaymentException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", exception.getMessage()));
  }
}
