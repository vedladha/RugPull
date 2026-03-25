package edu.wisc.t32.controller;

import com.example.demo.exception.DuplicateDisplayNameException;
import com.example.demo.exception.DuplicateEmailException;
import com.example.demo.exception.WalletProvisioningException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles auth-related exceptions for {@link AuthController}.
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthExceptionHandler.class);

  /**
   * Returns the existing 400 response body for duplicate registration data.
   *
   * @param exception the duplicate-data exception raised during registration
   * @return a bad request response with the original message field
   */
  @ExceptionHandler({DuplicateEmailException.class, DuplicateDisplayNameException.class})
  public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
    return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
  }

  /**
   * Returns the existing 503 response body for wallet provisioning failures.
   *
   * @param exception the wallet provisioning failure raised during registration
   * @return a service unavailable response with the existing error and details fields
   */
  @ExceptionHandler(WalletProvisioningException.class)
  public ResponseEntity<Map<String, String>> handleWalletProvisioningFailure(
      WalletProvisioningException exception) {
    LOGGER.error("Signup failed while creating wallet", exception);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
        "error", "Could not create wallet for new user",
        "details", exception.getMessage()));
  }
}
