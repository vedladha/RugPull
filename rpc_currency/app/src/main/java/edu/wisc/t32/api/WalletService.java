package edu.wisc.t32.api;

import edu.wisc.t32.impl.WalletServiceImpl;

/**
 * Represents a service that provides wallet interactions.
 *
 * <p>Do note that methods in this class may at any point make one or many  web requests.
 * As such these methods are inherently slow as IO is inherently involved.
 */
public interface WalletService extends AutoCloseable {

  /**
   * Creates a wallet for the $RPC service.
   *
   * @param initialFunding is an initial funding amount which is pulled from the operator of this
   *                       service
   * @return creates a new wallet account
   * @throws IllegalStateException thrown if a transaction fails in some way
   */
  Wallet createWallet(int initialFunding) throws IllegalStateException;

  /**
   * Creates a wallet from existing wallet information.
   *
   * @param accountId  an accountId
   * @param privateKey a privateKey
   * @return a wallet from the existing information
   * @throws IllegalArgumentException thrown if invalid arguments are passed to this method
   */
  Wallet createWallet(String accountId, String privateKey) throws IllegalArgumentException;

  /**
   * Creates a new wallet service for the given token id.
   *
   * @return the newly created wallet service
   */
  static WalletService getService(String operatorId, String operatorKey, String tokenId) {
    return WalletServiceImpl.create(operatorId, operatorKey, tokenId);
  }

}
