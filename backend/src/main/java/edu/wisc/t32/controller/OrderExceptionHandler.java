package edu.wisc.t32.controller;

import edu.wisc.t32.exception.InsufficientStockException;
import edu.wisc.t32.exception.OrderItemNotFoundException;
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
}
