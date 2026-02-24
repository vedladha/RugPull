package edu.wisc.t32;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountDeleteTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

/**
 * Defines an abstract test for all crypto-currency tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractCryptoTest {

  protected Client client;
  protected AccountId operatorId;
  protected PrivateKey operatorKey;

  @BeforeAll
  void setupAll() {
    this.operatorId = AccountId.fromString(System.getenv("OPERATOR_ID"));
    this.operatorKey = PrivateKey.fromStringECDSA(System.getenv("OPERATOR_KEY"));
    this.client = Client.forTestnet().setOperator(operatorId, operatorKey);
  }

  @AfterAll
  void teardownAll() {
    try {
      this.client.close();
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Spins up a test account for use in temporary testing.
   *
   * @return the test account
   */
  protected ClientPair spinTestAccount(int initialBalance) {
    PrivateKey key = PrivateKey.generateECDSA();
    var tx = new AccountCreateTransaction()
        .setAccountMemo("Test account")
        .setKeyWithAlias(key.getPublicKey())
        .setInitialBalance(new Hbar(initialBalance))
        .freezeWith(client)
        .sign(operatorKey);
    final var response = assertDoesNotThrow(() -> tx.execute(client), "Failed to spin test account.");
    final var receipt = assertDoesNotThrow(() -> response.getReceipt(client), "Failed to get receipt.");
    return new ClientPair(receipt.accountId, key, operatorId, client);
  }

  /**
   * A raw client pair for use internally.
   *
   * @param accountId  the Hedera accountId
   * @param privateKey the Hedera account privateKey
   */
  protected record ClientPair(AccountId accountId, PrivateKey privateKey, AccountId guarantor, Client client) implements Closeable {
    @Override
    public void close() throws IOException {
      var tx = new AccountDeleteTransaction()
          .setAccountId(accountId)
          .setTransferAccountId(guarantor)
          .freezeWith(client)
          .sign(privateKey);

      final var response = assertDoesNotThrow(() -> tx.execute(client));
      final var receipt = assertDoesNotThrow(() -> response.getReceipt(client));
      assertNull(receipt.accountId, "failed to delete account");
    }
  }
}
