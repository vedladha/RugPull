package edu.wisc.t32.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import edu.wisc.t32.AbstractCryptoTest;
import edu.wisc.t32.api.Wallet;
import edu.wisc.t32.api.WalletService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Test for the {@link WalletServiceImpl} implementation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WalletServiceImplTest extends AbstractCryptoTest {

  private TestToken token;
  private WalletService walletService;

  @BeforeAll
  void setupResources() {
    token = spinTestToken(10);
    walletService = assertDoesNotThrow(
        () -> WalletService.getService(this.operatorId.toString(), this.operatorKey.toString(),
            token.tokenId().toString()),
        "An exception occurred while creating the wallet service from known information");
  }

  @AfterAll
  void teardownResources() {
    try {
      walletService.close();
      token.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testWalletCreate() {
    // this uses are wallet service method
    Wallet wallet = assertDoesNotThrow(() -> this.walletService.createWallet(1),
        "The wallet should be successfully created, but was not");
    // below converts the accountId and privateKey into a ClientPair
    AccountId accountId = AccountId.fromString(wallet.getWalletId());
    PrivateKey key = PrivateKey.fromStringECDSA(wallet.getWalletPrivateKey());
    // this object is used to manage and delete our test account created via the Wallet object
    final ClientPair pair = new ClientPair(accountId, key, this.operatorId, this.client);
    assertDoesNotThrow(pair::close,
        "The created wallet should be successfully deleted, but was not");
  }

  @Test
  void testWalletCreateWithParams() {
    // again using the wallet service method
    Wallet wallet = assertDoesNotThrow(() -> this.walletService.createWallet(10),
        "Account creation should not throw, but an exception was thrown during the process");
    final String accountIdString = wallet.getWalletId();
    final String privateKeyString = wallet.getWalletPrivateKey();

    // re-init wallet with string information exposed through wallet class
    wallet = assertDoesNotThrow(
        () -> this.walletService.createWallet(accountIdString, privateKeyString),
        "Account creationg from known correct strings should not throw, but an "
            + "exception was thrown during the process");

    // now let's try to delete the re-init wallet
    AccountId accountId = AccountId.fromString(wallet.getWalletId());
    PrivateKey key = PrivateKey.fromStringECDSA(wallet.getWalletPrivateKey());

    // now we close like before by adding our data back into a ClientPair which is AutoClosable
    final ClientPair pair = new ClientPair(accountId, key, this.operatorId, this.client);
    assertDoesNotThrow(pair::close,
        "There was a failure closing the pair that was constructed through a re-init wallet");
  }
}
