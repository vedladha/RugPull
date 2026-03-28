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
import com.hedera.hashgraph.sdk.TokenTransfer;
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
}
