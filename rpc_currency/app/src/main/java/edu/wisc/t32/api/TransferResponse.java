package edu.wisc.t32.api;

/**
 * The result state of token the transfer.
 */
public enum TransferResponse {
  /**
   * Given if a transfer proceeds successfully.
   */
  SUCCESS,
  /**
   * Given for any other misc unknown failures during the transaction process.
   */
  FAILURE,
  /**
   * The given account is not accepting this type of token.
   *
   * <p>Note this error should almost never trigger. It is only possible when sending our token
   * outside of our custodial wallets. Which should sparsely if never happen.
   */
  NOT_ACCEPTING_TOKEN,
  /**
   * Given when the receiver account is an unknown account.
   */
  UNKNOWN_ACCOUNT,
  /**
   * The sender of this transaction has not enough balance to send.
   */
  INSUFFICIENT_BALANCE;
}
