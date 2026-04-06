package edu.wisc.t32.impl;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import edu.wisc.t32.api.Wallet;

/**
 * An implementation of the {@link Wallet} interface.
 *
 * @param accountId  the accountId of the hedera account
 * @param privateKey the privatekey of the hedera account
 */
public record WalletImpl(AccountId accountId, PrivateKey privateKey) implements Wallet {

  @Override
  public String getWalletId() {
    return this.accountId.toString();
  }

  @Override
  public String getWalletPrivateKey() {
    return this.privateKey.toString();
  }
}
