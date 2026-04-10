package edu.wisc.t32.impl;

import static edu.wisc.t32.impl.WalletUtils.assertNotNull;
import static edu.wisc.t32.impl.WalletUtils.floatToLong;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TokenMintTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import edu.wisc.t32.api.TokenSupplyService;
import edu.wisc.t32.api.Wallet;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeoutException;

/**
 * An implementation of the {@link TokenSupplyService} interface.
 */
public class TokenSupplyServiceImpl implements TokenSupplyService {

  private static final Gson GSON = new Gson();

  private final Client client;
  private final TokenId tokenId;
  private final PrivateKey supplyKey;
  private final AccountId treasury;
  private final int maxDecimals;

  /**
   * Sets up the TokenSupplyService to use the client and operator key with the provided supply key
   * and token.
   *
   * @param client      the client to use to send transactions
   * @param tokenId     the token id
   * @param supplyKey   the supply key for the token
   * @param maxDecimals the amount of decimals supported
   */
  public TokenSupplyServiceImpl(Client client, TokenId tokenId, PrivateKey supplyKey,
                                AccountId treasury, int maxDecimals) {
    this.client = client;
    this.tokenId = tokenId;
    this.supplyKey = supplyKey;
    this.treasury = treasury;
    this.maxDecimals = maxDecimals;
  }

  @Override
  public void mint(float amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("can not mint less than or 0 tokens");
    }
    long longAmount = floatToLong(amount, this.maxDecimals);

    TokenMintTransaction transaction = new TokenMintTransaction().setTokenId(this.tokenId)
        .setMaxTransactionFee(new Hbar(20)) // this is a just in case cap
        .setAmount(longAmount).freezeWith(this.client).sign(this.supplyKey);

    try {
      final TransactionResponse response = transaction.execute(this.client);
      // we don't need to check status from the receipt as hedera auto fails if it isn't a success.
    } catch (TimeoutException | PrecheckStatusException e) {
      throw new RuntimeException("Hedera Token minting failed" + e);
    }
  }

  @Override
  public void close() throws Exception {
    this.client.close();
  }

  private record PoolResponse(long supply, int decimals) {

    static PoolResponse EMPTY = new PoolResponse(0, 0);

    public boolean isEmpty() {
      return this == EMPTY;
    }

  }

  /**
   * Creates a new token supply service with the given parameters.
   *
   * @param operatorId  the operator account id used by the client
   * @param operatorKey the operator key used to sign transactions if required
   * @param tokenId     the token id to use.
   * @param supplyKey   the supply key used to authorize transactions.
   * @return a new supply service
   */
  public static TokenSupplyServiceImpl create(String operatorId, String operatorKey, String tokenId,
                                              String supplyKey) {
    assertNotNull(operatorId, "TokenSupplyServiceImpl", "operatorId", "create");
    assertNotNull(operatorKey, "TokenSupplyServiceImpl", "operatorKey", "create");
    assertNotNull(tokenId, "TokenSupplyServiceImpl", "tokenId", "create");
    assertNotNull(supplyKey, "TokenSupplyServiceImpl", "supplyKey", "create");

    final AccountId accountId = AccountId.fromString(operatorId);
    final PrivateKey privateKey = PrivateKey.fromStringECDSA(operatorKey);
    final TokenId token = TokenId.fromString(tokenId);
    final PrivateKey tokenKey = PrivateKey.fromStringECDSA(supplyKey);

    final Client client = Client.forTestnet().setOperator(accountId, privateKey);

    try {
      final TokenInfo tokenInfo = new TokenInfoQuery().setTokenId(token).execute(client);
      return new TokenSupplyServiceImpl(client, token, tokenKey, tokenInfo.treasuryAccountId,
          tokenInfo.decimals);
    } catch (TimeoutException | PrecheckStatusException e) {
      throw new RuntimeException(e);
    }
  }

  // expose for testing
  public AccountId getTreasury() {
    return treasury;
  }
}
