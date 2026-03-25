package com.example.demo.exception;

/**
 * Thrown when wallet provisioning fails during registration.
 */
public class WalletProvisioningException extends RuntimeException {

  /**
   * Constructs the exception with the provisioning failure message and cause.
   *
   * @param message the message describing the wallet provisioning failure
   * @param cause the original provisioning exception
   */
  public WalletProvisioningException(String message, Throwable cause) {
    super(message, cause);
  }
}
