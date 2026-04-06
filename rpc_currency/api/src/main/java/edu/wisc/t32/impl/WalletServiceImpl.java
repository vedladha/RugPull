package edu.wisc.t32.impl;

import static edu.wisc.t32.impl.WalletUtils.assertNotNull;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import edu.wisc.t32.api.TransferResponse;
import edu.wisc.t32.api.Wallet;
import edu.wisc.t32.api.WalletService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the {@link WalletService} interface.
 */
public class WalletServiceImpl implements WalletService {
  private static Logger LOGGER = LoggerFactory.getLogger(WalletServiceImpl.class);

  private final Client client;
  private final PrivateKey operatorKey;
  private final TokenId tokenId;
  private final int maxDecimals;

  /**
   * Sets up the RpcWalletService to use the client and operatorKey.
   *
   * @param client      the hedera client
   * @param operatorKey the operator key to sign misc transactions with.
   * @param tokenId     the token id of the wallet service
   */
  public WalletServiceImpl(Client client, PrivateKey operatorKey, TokenId tokenId,
                           int maxDecimals) {
    this.client = client;
    this.operatorKey = operatorKey;
    this.tokenId = tokenId;
    this.maxDecimals = maxDecimals;
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
      // transfer tokens from the Operator Account to the newly created account
      receipt =
          transferToken(this.client.getOperatorAccountId(), this.operatorKey, accountId,
              initalFunding);
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
  public TransferResponse transferBalance(Wallet sender, Wallet receiver, float amount)
      throws IllegalArgumentException, IllegalStateException {
    assertNotNull(sender, "WalletServiceImpl", "sender", "transferBalance");
    assertNotNull(receiver, "WalletServiceImpl", "receiver", "transferBalance");
    if (sender.getWalletId().equals(receiver.getWalletId())) {
      throw new IllegalArgumentException("Can not transfer tokens between the same wallet");
    }

    if (amount <= 0) {
      throw new IllegalArgumentException(
          "The specified amount of a given transfer must be greater than 0");
    }

    BigDecimal decimal = new BigDecimal(Float.toString(amount));
    long transferAmount;
    // we do this to get rid of all decimals. We can have at most 2 so throw if invalid input
    try {
      transferAmount = decimal.movePointRight(this.maxDecimals).longValueExact();
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException("input amount is larger than 2 decimal places");
    }

    // now we can start our tx
    final WalletImpl txSender = (WalletImpl) sender;
    final WalletImpl txReceiver = (WalletImpl) receiver;

    final TransferTransaction tokenTransferTx = new TransferTransaction()
        .addTokenTransferWithDecimals(
            this.tokenId, txSender.accountId(), -transferAmount, this.maxDecimals
        ).addTokenTransferWithDecimals(
            this.tokenId, txReceiver.accountId(), transferAmount, this.maxDecimals
        ).freezeWith(this.client).sign(txSender.privateKey());

    try {
      final TransactionResponse response = tokenTransferTx.execute(this.client);
      response.setValidateStatus(false);
      final TransactionReceipt receipt = response.getReceipt(this.client);
      LOGGER.info("Completed transaction {} by sending amount {} with {} decimals",
          receipt.transactionId, transferAmount, this.maxDecimals);

      // maps hedera status to the api TransferResponse
      return switch (receipt.status) {
        case SUCCESS -> TransferResponse.SUCCESS;
        case INSUFFICIENT_TOKEN_BALANCE -> TransferResponse.INSUFFICIENT_BALANCE;
        case ACCOUNT_DELETED, INVALID_ACCOUNT_ID -> TransferResponse.UNKNOWN_ACCOUNT;
        case TOKEN_NOT_ASSOCIATED_TO_ACCOUNT -> TransferResponse.NOT_ACCEPTING_TOKEN;
        default -> {
          System.out.println(receipt.status);
          LOGGER.error(
              "Failed to map hedera status to api TransferResponse for hedera status code {}",
              receipt.status);
          yield TransferResponse.FAILURE;
        }
      };
    } catch (TimeoutException | PrecheckStatusException | ReceiptStatusException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public float getBalance(Wallet wallet) throws IllegalArgumentException, IllegalStateException {
    assertNotNull(wallet, "WalletServiceImpl", "wallet", "getBalance");

    final AccountId accountId = AccountId.fromString(wallet.getWalletId());

    try {
      final var accountInfo = new AccountInfoQuery().setAccountId(accountId).execute(this.client);
      final var relationship = accountInfo.tokenRelationships.get(this.tokenId);
      if (relationship == null) {
        return 0;
      }
      long balance = relationship.balance;
      BigDecimal decimalized = new BigDecimal(balance).movePointLeft(relationship.decimals);
      return decimalized.floatValue();
    } catch (TimeoutException | PrecheckStatusException e) {
      throw new IllegalStateException(e);
    }
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

    try {
      final TokenInfo tokenInfo = new TokenInfoQuery().setTokenId(token).execute(client);
      return new WalletServiceImpl(client, privateKey, token, tokenInfo.decimals);
    } catch (TimeoutException | PrecheckStatusException e) {
      throw new RuntimeException(e);
    }
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
        new TransferTransaction().addTokenTransfer(this.tokenId, fromAccount, -amount)
            .addTokenTransfer(this.tokenId, toAccount, amount).freezeWith(this.client)
            .sign(fromAccountKey);

    try {
      final TransactionResponse response = transferTransaction.execute(this.client);
      return response.getReceipt(this.client);
    } catch (TimeoutException | PrecheckStatusException | ReceiptStatusException e) {
      throw new RuntimeException(e);
    }
  }
}
