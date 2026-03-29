package edu.wisc.t32.api;

import edu.wisc.t32.impl.TokenSupplyServiceImpl;

/**
 * The token service takes in a token id, and supply key. To allow you to make modifications to a
 * token such as increasing supply or decreasing supply of the token.
 */
public interface TokenSupplyService {
  /**
   * Mints the amount of tokens specified. for the token this supply service acts on behalf of.
   *
   * <p>Note a token may only allow for X amount of decimal support this method will throw if you
   * exceed the amount of decimal places allowed by the token when attempting to mint.
   *
   * @param amount the amount of tokens to mint
   * @throws IllegalArgumentException thrown if the parameter is negative or 0
   */
  void mint(float amount) throws IllegalArgumentException;

  /**
   * Exchanges the token defined by this supply service for the provided amount of hbar.
   *
   * <p>HBar is the default Hedera currency type. There is a pinned exchange rate used by any
   * exchange method see {@link #exchange(Wallet, String, float)} for definition information.
   *
   * @param exchanger  the wallet who is exchanging hbar for some amount of token.
   * @param hbarAmount the amount of hbar to exchange
   * @return the amount of this token received. A non-zero float.
   * @throws IllegalArgumentException thrown for invalid wallet or negative or 0 hbar amount. Also
   *                                  thrown if hbarAmount exceeds 8 decimals.
   */
  float exchangeHbar(Wallet exchanger, float hbarAmount) throws IllegalArgumentException;

  /**
   * Exchanges the provided tokenId from this exchanger account to this token.
   *
   * <p>The formula is defined as Price of A = Reserve B / Reserve A
   *
   * @param exchanger   the exchanger trading provided tokenId for this token.
   * @param tokenId     the tokenId
   * @param tokenAmount the amount of token to transact
   * @return the amount of this token received. A non-zero float.
   * @throws IllegalArgumentException thrown for invalid wallet, negative 0 token amount. Also
   *                                  thrown if tokenAmount exceeds maximum allowe ddecimals.
   */
  float exchange(Wallet exchanger, String tokenId, float tokenAmount)
      throws IllegalArgumentException;

  /**
   * Creates a new token supply service with the given parameters.
   *
   * @param operatorId  the operator account id used by the client
   * @param operatorKey the operator key used to sign transactions if required
   * @param tokenId     the token id to use.
   * @param supplyKey   the supply key used to authorize transactions.
   * @return a new supply service
   */
  static TokenSupplyService getService(String operatorId, String operatorKey, String tokenId,
                                       String supplyKey) {
    return TokenSupplyServiceImpl.create(operatorId, operatorId, tokenId, supplyKey);
  }
}
