package edu.wisc.t32.enums;

/**
 * Represents the order status of a
 * {@link edu.wisc.t32.model.Order}.
 * Used to track fulfillment of a purchase.
 */
public enum OrderStatus {
  /**
   * Order created, but payment process has not been
   * initiated yet.
   */
  PENDING,

  /**
   * The transaction has been submitted, awaiting a
   * success receipt on payment.
   */
  AWAITING_CONFIRMATION,

  /**
   * Payment has been completed, order payment has been
   * fulfilled.
   */
  COMPLETED,

  /**
   * The user manually cancelled before the transaction was
   * sent.
   */
  CANCELLED,

  /**
   * The transaction was sent but failed on the network.
   */
  FAILED
}
