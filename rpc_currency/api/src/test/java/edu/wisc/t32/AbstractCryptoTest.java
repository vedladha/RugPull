package edu.wisc.t32;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountDeleteTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenDeleteTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransferTransaction;
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
    final var response =
        assertDoesNotThrow(() -> tx.execute(client), "Failed to spin test account.");
    final var receipt =
        assertDoesNotThrow(() -> response.getReceipt(client), "Failed to get receipt.");
    return new ClientPair(receipt.accountId, key, operatorId, client);
  }

  /**
   * Spins up a test token that can be closed.
   *
   * @param initialSupply the test supply
   * @return the test token
   */
  protected TestToken spinTestToken(int initialSupply) {
    PrivateKey adminKey = PrivateKey.generateECDSA();
    var tx = new TokenCreateTransaction()
        .setInitialSupply(initialSupply)
        .setDecimals(2)
        .setTokenName("TEST")
        .setTokenSymbol("TST")
        .setTreasuryAccountId(this.operatorId)
        .setAdminKey(adminKey)
        .setSupplyKey(adminKey)
        .freezeWith(this.client)
        .sign(adminKey);

    final var response = assertDoesNotThrow(() -> tx.execute(this.client));
    final var receipt = assertDoesNotThrow(() -> response.getReceipt(client));
    assertNotNull(receipt.tokenId);
    return new TestToken(this.client, receipt.tokenId, adminKey);
  }

  /**
   * A raw client pair for use internally.
   *
   * @param accountId  the Hedera accountId
   * @param privateKey the Hedera account privateKey
   */
  public record ClientPair(AccountId accountId, PrivateKey privateKey, AccountId guarantor,
                           Client client) implements AutoCloseable {
    @Override
    public void close() throws Exception {
      // before we delete the account we have to wipe all associated tokens and send them back
      // to our treasurer
      var infoTx = new AccountInfoQuery()
          .setAccountId(accountId)
          .execute(this.client);
      infoTx.tokenRelationships.forEach((token, relation) -> {
        // this transfers everything to our treasuerer
        var transferTx = new TransferTransaction()
            .addTokenTransfer(token, accountId, -relation.balance)
            .addTokenTransfer(token, guarantor, relation.balance)
            .freezeWith(this.client)
            .sign(privateKey);
        final var response = assertDoesNotThrow(() -> transferTx.execute(this.client));
        final var receipt = assertDoesNotThrow(() -> response.getReceipt(this.client));
        assertEquals(Status.SUCCESS, receipt.status);
      });

      // now we can finally delete teh account
      var deleteTx = new AccountDeleteTransaction()
          .setAccountId(accountId)
          .setTransferAccountId(guarantor)
          .freezeWith(client)
          .sign(privateKey);

      final var response = assertDoesNotThrow(() -> deleteTx.execute(client));
      final var receipt = assertDoesNotThrow(() -> response.getReceipt(client));
      assertNull(receipt.accountId, "failed to delete account");
    }
  }

  /**
   * A TestCoin for internal testing use.
   *
   * @param tokenId  the Hedera tokenId
   * @param adminKey the Hedera adminKey
   */
  protected record TestToken(Client client, TokenId tokenId, PrivateKey adminKey)
      implements AutoCloseable {

    @Override
    public void close() throws Exception {
      var tx = new TokenDeleteTransaction()
          .setTokenId(tokenId)
          .freezeWith(client)
          .sign(this.adminKey);

      final var response = assertDoesNotThrow(() -> tx.execute(client));
      final var receipt = assertDoesNotThrow(() -> response.getReceipt(client));
      assertNull(receipt.tokenId);
    }
  }
}
