package edu.wisc.t32.impl;

import static edu.wisc.t32.impl.WalletUtils.assertNotNull;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import edu.wisc.t32.api.Wallet;
import edu.wisc.t32.api.WalletService;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * An implementation of the {@link WalletService} interface.
 */
public class WalletServiceImpl implements WalletService {

  private final Client client;
  private final PrivateKey operatorKey;
  private final TokenId tokenId;

  /**
   * Sets up the RpcWalletService to use the client and operatorKey.
   *
   * @param client      the hedera client
   * @param operatorKey the operator key to sign misc transactions with.
   * @param tokenId     the token id of the wallet service
   */
  public WalletServiceImpl(Client client, PrivateKey operatorKey, TokenId tokenId) {
    this.client = client;
    this.operatorKey = operatorKey;
    this.tokenId = tokenId;
  }

  @Override
  public Wallet createWallet(int initalFunding) {
    final PrivateKey privateKey = PrivateKey.generateECDSA();
    final AccountCreateTransaction accountCreateTransaction =
        new AccountCreateTransaction().setKeyWithAlias(privateKey.getPublicKey())
            .setInitialBalance(new Hbar(0)).freezeWith(this.client).sign(this.operatorKey);

    final AccountId accountId;
    try {
      TransactionResponse response = accountCreateTransaction.execute(client);
      TransactionReceipt receipt = response.getReceipt(client);
      if (receipt.status != Status.SUCCESS) {
        throw new IllegalStateException(
            "The wallet was not created successfully. got status code " + receipt.status
        );
      }
      accountId = receipt.accountId;
    } catch (TimeoutException | PrecheckStatusException | ReceiptStatusException e) {
      throw new RuntimeException(e);
    }

    TransactionReceipt receipt = associateToken(accountId, privateKey);
    if (receipt.status != Status.SUCCESS) {
      throw new IllegalStateException(
          "Failed to associate token with account %s. Instead got status code %s"
              .formatted(accountId, receipt.status)
      );
    }

    if (initalFunding > 0) {
      receipt =
          transferToken(accountId, privateKey, this.client.getOperatorAccountId(), initalFunding);
      if (receipt.status != Status.SUCCESS) {
        throw new IllegalStateException(
            "The wallet %s failed to receive initial funding. got status code %s"
                .formatted(accountId, receipt.status)
        );
      }
    }

    return new WalletImpl(accountId, privateKey);
  }

  @Override
  public Wallet createWallet(String accountId, String privateKey) throws IllegalArgumentException {
    assertNotNull(accountId, "WalletServiceImpl", "accountId", "createWallet");
    assertNotNull(privateKey, "WalletServiceImpl", "privateKey", "createWallet");

    PrivateKey hederaKey = PrivateKey.fromStringDER(privateKey);
    AccountId hederaId = AccountId.fromString(accountId);
    return new WalletImpl(hederaId, hederaKey);
  }

  @Override
  public void close() throws Exception {
    this.client.close();
  }

  /**
   * Creates a wallet service from a given token id.
   *
   * @param operatorId  the id of the wallet operator
   * @param operatorKey the private key of the client operator
   * @param tokenId     the token id to use for this service
   * @return the wallet service
   */
  public static WalletServiceImpl create(String operatorId, String operatorKey, String tokenId) {
    assertNotNull(operatorId, "WalletServiceImpl", "operatorId", "create");
    assertNotNull(operatorKey, "WalletServiceImpl", "operatorKey", "create");
    assertNotNull(tokenId, "WalletServiceImpl", "tokenId", "create");

    final AccountId accountId = AccountId.fromString(operatorId);
    final PrivateKey privateKey = PrivateKey.fromStringECDSA(operatorKey);
    final Client client = Client.forTestnet().setOperator(accountId, privateKey);
    final TokenId token = TokenId.fromString(tokenId);

    return new WalletServiceImpl(client, privateKey, token);
  }

  /**
   * Associates the token that this service is created for to the specified account.
   *
   * @param accountId  the accountId to associate with this token
   * @param privateKey the privateKey of the given accountId
   * @return the receipt, which contains various important transaction metadata
   * @throws RuntimeException thrown if there is an error during the transaction
   */
  private TransactionReceipt associateToken(AccountId accountId, PrivateKey privateKey) {
    final TokenAssociateTransaction accountAssociateTransaction =
        new TokenAssociateTransaction().setAccountId(accountId).setTokenIds(List.of(this.tokenId))
            .freezeWith(this.client).sign(privateKey);

    try {
      TransactionResponse response = accountAssociateTransaction.execute(client);
      return response.getReceipt(client);
    } catch (TimeoutException | PrecheckStatusException | ReceiptStatusException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Transfers a token of the specified amount to the fromAccount to the toAccount.
   *
   * @param fromAccount    the account sending the currency
   * @param fromAccountKey the private signing key of the account sending the currency
   * @param toAccount      the account that will be receiving the currency
   * @param amount         the amount of currency to send
   * @return the receipt, which contains various important transaction metadata
   * @throws RuntimeException thrown if there is an error during the transaction
   */
  private TransactionReceipt transferToken(AccountId fromAccount, PrivateKey fromAccountKey,
                                           AccountId toAccount, long amount)
      throws RuntimeException {
    final TransferTransaction transferTransaction =
        new TransferTransaction().addTokenTransfer(this.tokenId, fromAccount, amount)
            .addTokenTransfer(this.tokenId, toAccount, -amount).freezeWith(this.client)
            .sign(fromAccountKey);

    try {
      final TransactionResponse response = transferTransaction.execute(this.client);
      return response.getReceipt(this.client);
    } catch (TimeoutException | PrecheckStatusException | ReceiptStatusException e) {
      throw new RuntimeException(e);
    }
  }
}
