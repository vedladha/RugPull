package edu.wisc.t32;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The launcher for the $RPC crypto currency.
 *
 * <p>The main class in this application is not required to be running all the time instead this app
 * acts as a deployment for various crypto-currency needs on a single time run basis.
 */
public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  private static final String TOKEN_NAME = "Rug Pull Coin";
  private static final String TOKEN_SYMBOL = "RPC";
  private static final int DECIMAL_LENGTH = 2;
  private static final int INITIAL_SUPPLY = 500_00;

  /**
   * Launches the $RPC deployment application.
   *
   * @param args the command line arguments given to the $RPC deployment
   */
  @SuppressWarnings("null")
  public static void main(String[] args) {
    AccountId operatorId = AccountId.fromString(getenvOrThrow("OPERATOR_ID"));
    PrivateKey operatorKey = PrivateKey.fromStringECDSA(getenvOrThrow("OPERATOR_KEY"));

    try (Client client = Client.forTestnet().setOperator(operatorId, operatorKey)) {
      final CryptoCreationResult createResult = createCurrency(client, Path.of("rpc_keys.txt"));
      if (createResult.status == Status.SUCCESS && createResult.tokenId != null) {
        LOGGER.info("Token created with id {}", createResult.tokenId);
      } else {
        LOGGER.info("Failed to create token. Creation returned with result status {}",
            createResult.status);
      }
    } catch (Exception e) {
      LOGGER.error("Token creation failed.", e);
    }
  }

  /**
   * Creates the RPC crypto currency and dumps its contents to a file if the function is set.
   *
   * @param keyFile the file to dump the keys into, or null to not dump keys.
   * @return the crypto result see {@link CryptoCreationResult}
   */
  public static CryptoCreationResult createCurrency(Client client, Path keyFile)
      throws PrecheckStatusException, TimeoutException, ReceiptStatusException, IOException {
    PrivateKey adminKey = PrivateKey.generateED25519();
    PrivateKey supplyKey = PrivateKey.generateED25519();

    TokenCreateTransaction transaction =
        new TokenCreateTransaction()
            .setTokenName(TOKEN_NAME)
            .setTokenSymbol(TOKEN_SYMBOL)
            .setDecimals(DECIMAL_LENGTH)
            .setInitialSupply(INITIAL_SUPPLY)
            .setTreasuryAccountId(client.getOperatorAccountId())
            .setAdminKey(adminKey).setSupplyKey(supplyKey)
            .freezeWith(client).sign(adminKey);

    TransactionResponse response = transaction.execute(client);
    TransactionReceipt receipt = response.getReceipt(client);

    if (keyFile != null) {
      Files.writeString(keyFile,
          "tokenId=" + receipt.tokenId
              + System.lineSeparator()
              + "adminKey=" + adminKey
              + System.lineSeparator()
              + "supplyKey=" + supplyKey
              + System.lineSeparator());
      LOGGER.info("Saved adminKey and supplyKey to {}", keyFile);
    }

    return new CryptoCreationResult(adminKey, supplyKey, receipt.tokenId, receipt.status);
  }

  /**
   * Represents a result of a crypto currency creation. Carries important information that should
   * not be discarded upon creation.
   *
   * @param adminKey  the key that has full control over token config and properties
   * @param supplyKey the key that controls minting (increasing supply) and burning
   *                  (decreasing supply) of tokens
   * @param tokenId   The id of the created token
   * @param status    the status of the transaction
   */
  public record CryptoCreationResult(PrivateKey adminKey, PrivateKey supplyKey, TokenId tokenId,
                                     Status status) {

  }

  /**
   * Gets an environment variable from the environment, but throws if it does not exist.
   *
   * @param key the environment variable key
   * @return the environment variable
   * @throws IllegalArgumentException thrown if the environment variable can't be found
   */
  private static String getenvOrThrow(String key) throws IllegalArgumentException {
    final String out = System.getenv(key);
    if (out == null) {
      throw new IllegalArgumentException(
          "Could not find the environment variable %s is the environment".formatted(key));
    }

    return out;
  }
}
