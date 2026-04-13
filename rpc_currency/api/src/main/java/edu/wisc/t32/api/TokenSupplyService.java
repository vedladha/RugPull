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
