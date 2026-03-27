package edu.wisc.t32.enums;

/**
 * Represents the account status of a
 * {@link edu.wisc.t32.model.User}.
 * Used to track the synchronization state with Hedera testnet.
 */
public enum UserStatus {
  /**
   * Account has been created but is awaiting verification
   * with Hedera testnet.
   */
  PENDING,

  /**
   * Account is created and fully verified.
   */
  ACTIVE,

  /**
   * Account creation or synchronization process
   * encountered an error.
   */
  FAILED
}
