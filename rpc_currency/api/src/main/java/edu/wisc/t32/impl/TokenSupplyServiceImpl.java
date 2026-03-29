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
                                int maxDecimals) {
    this.client = client;
    this.tokenId = tokenId;
    this.supplyKey = supplyKey;
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
  public float exchangeHbar(Wallet exchanger, float hbarAmount)
      throws IllegalArgumentException {
    assertNotNull(exchanger, "TokenSupplyServiceImpl", "exchanger", "exchangeHbar");
    if (hbarAmount <= 0.0f) {
      throw new IllegalArgumentException("HBAR amount must be greater than 0");
    }

    // as specified null means use hbar
    return executeSwap(exchanger, null, hbarAmount);
  }

  @Override
  public float exchange(Wallet exchanger, String tokenId, float tokenAmount)
      throws IllegalArgumentException {
    assertNotNull(exchanger, "TokenSupplyServiceImpl", "exchanger", "exchange");
    assertNotNull(tokenId, "TokenSupplyServiceImpl", "tokenId", "exchange");
    if (tokenAmount <= 0.0f) {
      throw new IllegalArgumentException("Token amount must be greater than 0");
    }

    return executeSwap(exchanger, TokenId.fromString(tokenId), tokenAmount);
  }

  /**
   * Internal backend for swapping tokens, null tokenId means use hbar.
   *
   * @param exchanger   The wallet executing the swap.
   * @param fromTokenId The token being given. or null for hbar
   * @param amountIn    The amount of tokens being given.
   * @return The amount of this token received.
   */
  private float executeSwap(Wallet exchanger, TokenId fromTokenId, float amountIn) {
    int decimalsIn;
    BigDecimal reserveIn;

    if (fromTokenId == null) {
      decimalsIn = 8;
      long hbarTinybars = getTreasuryHbarBalance(this.client);
      reserveIn = new BigDecimal(hbarTinybars).movePointLeft(decimalsIn);
    } else {
      PoolResponse fromToken = getPoolReserve(this.client, fromTokenId);
      decimalsIn = fromToken.decimals;
      reserveIn = new BigDecimal(fromToken.supply).movePointLeft(decimalsIn);
    }

    // below is just a lot of math ins hort what we are doing is "normalizing our decimals"
    // This basically means if our currencies support different amount of decimals we have to
    // ensure that our tokens "line up" or are normalized with eachother.
    PoolResponse toToken = getPoolReserve(this.client, this.tokenId);
    BigDecimal reserveOut = new BigDecimal(toToken.supply).movePointLeft(toToken.decimals);

    if (reserveIn.compareTo(BigDecimal.ZERO) == 0 || toToken.supply == 0) {
      throw new IllegalStateException("Insufficient pool liquidity to execute a currency swap");
    }

    BigDecimal amountInBd = new BigDecimal(Float.toString(amountIn));
    BigDecimal numerator = reserveOut.multiply(amountInBd);
    BigDecimal denominator = reserveIn.add(amountInBd);

    BigDecimal amountOutBd = numerator.divide(denominator, toToken.decimals, RoundingMode.DOWN);

    long rawAmountIn = amountInBd.movePointRight(decimalsIn).longValueExact();
    long rawAmountOut = amountOutBd.movePointRight(toToken.decimals).longValueExact();

    if (rawAmountOut <= 0) {
      throw new IllegalArgumentException("Exchange amount too small to yield any received tokens.");
    }

    final WalletImpl wallet = (WalletImpl) exchanger;
    final AccountId poolAccount = this.client.getOperatorAccountId();
    final AccountId userAccount = wallet.accountId();

    final TransferTransaction transferTx = new TransferTransaction();

    if (fromTokenId == null) {
      transferTx.addHbarTransfer(userAccount, Hbar.fromTinybars(-rawAmountIn))
          .addHbarTransfer(poolAccount, Hbar.fromTinybars(rawAmountIn));
    } else {
      transferTx.addTokenTransferWithDecimals(fromTokenId, userAccount, -rawAmountIn, decimalsIn)
          .addTokenTransferWithDecimals(fromTokenId, poolAccount, rawAmountIn, decimalsIn);
    }

    transferTx.addTokenTransferWithDecimals(this.tokenId, poolAccount, -rawAmountOut,
            toToken.decimals)
        .addTokenTransferWithDecimals(this.tokenId, userAccount, rawAmountOut, toToken.decimals);

    transferTx.freezeWith(this.client).sign(wallet.privateKey()).signWithOperator(this.client);

    try {
      final TransactionResponse response = transferTx.execute(this.client).setValidateStatus(false);
      final TransactionReceipt receipt = response.getReceipt(this.client);

      if (receipt.status != Status.SUCCESS) {
        throw new IllegalStateException("Exchange failed with network Status: " + receipt.status);
      }
    } catch (TimeoutException | PrecheckStatusException | ReceiptStatusException e) {
      throw new RuntimeException("Network error during exchange execution", e);
    }

    return amountOutBd.floatValue();
  }

  private long getTreasuryHbarBalance(Client client) {
    try (HttpClient httpClient = HttpClient.newHttpClient()) {
      String url = "https://testnet.mirrornode.hedera.com/api/v1/balances?account.id="
          + client.getOperatorAccountId().toString();

      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException("Mirror node returned status code: " + response.statusCode());
      }

      JsonObject rootJson = GSON.fromJson(response.body(), JsonObject.class);
      if (!rootJson.has("balances") || rootJson.getAsJsonArray("balances").isEmpty()) {
        return 0L;
      }

      JsonObject accountData = rootJson.getAsJsonArray("balances").get(0).getAsJsonObject();
      if (accountData.has("balance")) {
        return accountData.get("balance").getAsLong();
      }
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Failed to fetch HBAR reserves from the Hedera network", e);
    }
    return 0L;
  }

  private static PoolResponse getPoolReserve(Client client, TokenId tokenId) {
    try {
      TokenInfo token = new TokenInfoQuery().setTokenId(tokenId).execute(client);

      try (HttpClient httpClient = HttpClient.newHttpClient()) {

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(
            "https://testnet.mirrornode.hedera.com/api/v1/balances?account.id=%s".formatted(
                token.treasuryAccountId.toString()))).GET().build();

        // see https://testnet.mirrornode.hedera.com/api/v1/balances?account.id=0.0.7928809
        // for example structure
        String rawJson = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        JsonObject json = GSON.fromJson(rawJson, JsonObject.class);
        if (!json.has("balances")) {
          return PoolResponse.EMPTY;
        }

        json = json.getAsJsonArray("balances").get(0).getAsJsonObject();

        if (!json.has("tokens")) {
          return PoolResponse.EMPTY;
        }

        String targetId = tokenId.toString();
        for (JsonElement element : json.getAsJsonArray("tokens")) {
          final JsonObject entry = element.getAsJsonObject();
          final String id = entry.get("token_id").getAsString();
          if (id.equals(targetId)) {
            long balance = entry.get("balance").getAsLong();
            return new PoolResponse(balance, token.decimals);
          }
        }
      }
    } catch (TimeoutException | PrecheckStatusException | IOException | InterruptedException e) {
      throw new RuntimeException("Failed to fetch pool reserves from the Hedera network", e);
    }

    return PoolResponse.EMPTY;
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
      return new TokenSupplyServiceImpl(client, token, tokenKey, tokenInfo.decimals);
    } catch (TimeoutException | PrecheckStatusException e) {
      throw new RuntimeException(e);
    }
  }

  private record PoolResponse(long supply, int decimals) {
    static PoolResponse EMPTY = new PoolResponse(0, 0);

    public boolean isEmpty() {
      return this == EMPTY;
    }
  }
}
