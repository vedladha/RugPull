package edu.wisc.t32.api;

/**
 * Represents a wallet which can hold, transfer, and receive $RPC.
 *
 * @version 1.0.0
 */
public interface Wallet {

  /**
   * Gets the account id of this wallet.
   *
   * <p>This id should not be usually read directly instead being used for comparison and
   * identification only.
   *
   * @return the string that represents the wallets account id.
   */
  String getWalletId();

  /**
   * Gets the private key of this wallet.
   *
   * <p>A wallet private key is used to sign and authorize transactions for a specific wallet. This
   * should be treated as a password and sensitive information. It may be the case that this method
   * returns null.
   *
   * @return the private key as a string.
   */
  String getWalletPrivateKey();

}
