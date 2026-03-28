package edu.wisc.t32.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import edu.wisc.t32.AbstractCryptoTest;
import edu.wisc.t32.api.TransferResponse;
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
    token = spinTestToken(1000_00);
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

    // we will use our teardown method which automatically helps us destroy this account
    teardownWallet(wallet);
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
    teardownWallet(wallet);
  }

  @Test
  void testGetBalance() {
    final long initialFunding = 7_50;
    final float initalFundingFloat = 7.50f;
    final Wallet wallet =
        assertDoesNotThrow(() -> this.walletService.createWallet((int) initialFunding),
            "Wallet creation with funding should not throw");

    final float balance = assertDoesNotThrow(() -> this.walletService.getBalance(wallet),
        "Wallet balance query should not throw");
    assertEquals(initalFundingFloat, balance,
        "getBalance should return the wallet's initial funded balance");

    teardownWallet(wallet);
  }

  @Test
  void testCreateWalletInitialFundingDirection() {
    final long initialFunding = 10_00;
    final float initialFundingDecimal = 10.00f;
    final Wallet operatorWallet = new WalletImpl(this.operatorId, this.operatorKey);

    final float operatorStartBalance =
        assertDoesNotThrow(() -> this.walletService.getBalance(operatorWallet),
            "Wallet balance query should not throw");
    final Wallet wallet =
        assertDoesNotThrow(() -> this.walletService.createWallet((int) initialFunding),
            "Wallet creation with funding should succeed");

    final float walletBalance = assertDoesNotThrow(() -> this.walletService.getBalance(wallet),
        "Wallet balance query should not throw");
    final float operatorEndBalance =
        assertDoesNotThrow(() -> this.walletService.getBalance(operatorWallet),
            "Wallet balance query should not throw");

    assertEquals(initialFundingDecimal, walletBalance,
        "The new wallet should receive the requested initial funding");
    assertEquals(operatorStartBalance - initialFundingDecimal, operatorEndBalance,
        "Operator should pay the wallet initial funding");

    teardownWallet(wallet);
  }

  @Test
  void testTransferBalanceDirection() {
    final long senderStartTokens = 100_00;
    final float transferAmount = 10.00f;

    final Wallet sender =
        assertDoesNotThrow(() -> this.walletService.createWallet((int) senderStartTokens),
            "Sender wallet should be created");
    final Wallet receiver = assertDoesNotThrow(() -> this.walletService.createWallet(0),
        "Receiver wallet should be created");

    final float senderBalanceBefore =
        assertDoesNotThrow(() -> this.walletService.getBalance(sender),
            "Wallet balance query should not throw");
    final float receiverBalanceBefore =
        assertDoesNotThrow(() -> this.walletService.getBalance(receiver),
            "Wallet balance query should not throw");

    final TransferResponse response = assertDoesNotThrow(
        () -> this.walletService.transferBalance(sender, receiver, transferAmount),
        "Transfer should not throw");
    assertEquals(TransferResponse.SUCCESS, response,
        "Expected transfer to succeed when sender has sufficient balance");

    final float senderBalanceAfter = assertDoesNotThrow(() -> this.walletService.getBalance(sender),
        "Wallet balance query should not throw");
    final float receiverBalanceAfter =
        assertDoesNotThrow(() -> this.walletService.getBalance(receiver),
            "Wallet balance query should not throw");

    assertEquals(senderBalanceBefore - transferAmount, senderBalanceAfter,
        "Sender balance should decrease by transfer amount");
    assertEquals(receiverBalanceBefore + transferAmount, receiverBalanceAfter,
        "Receiver balance should increase by transfer amount");

    teardownWallet(sender);
    teardownWallet(receiver);
  }

  @Test
  void testWalletValidTransaction() {
    final Wallet sender = assertDoesNotThrow(() -> this.walletService.createWallet(100_00),
        "assertion on account creation failed");
    final Wallet receiver = assertDoesNotThrow(() -> this.walletService.createWallet(1_00),
        "assertion on account creation failed");

    final TransferResponse response =
        assertDoesNotThrow(() -> this.walletService.transferBalance(sender, receiver, 10.0f),
            "The expected successful transfer did not complete successfully as "
                + "there was some network error");
    assertEquals(TransferResponse.SUCCESS, response,
        "The transfer failed when response, but SUCCESS was expected");

    // teardown our wallet accounts used for this test
    teardownWallet(sender);
    teardownWallet(receiver);
  }

  @Test
  void testWalletValidTransactionWithDecimals() {
    final Wallet sender = assertDoesNotThrow(() -> this.walletService.createWallet(100_00),
        "assertion on account creation failed");
    final Wallet receiver = assertDoesNotThrow(() -> this.walletService.createWallet(1_00),
        "assertion on account creation failed");

    TransferResponse response =
        assertDoesNotThrow(() -> this.walletService.transferBalance(sender, receiver, 10.1f),
            "The expected successful transfer did not complete successfully as "
                + "there was some network error");
    assertEquals(TransferResponse.SUCCESS, response,
        "The transfer failed when response, but SUCCESS was expected");

    response =
        assertDoesNotThrow(() -> this.walletService.transferBalance(sender, receiver, 10.11f),
            "The expected successful transfer did not complete successfully as "
                + "there was some network error");
    assertEquals(TransferResponse.SUCCESS, response,
        "The transfer failed when response, but SUCCESS was expected");


    // teardown our wallet accounts used for this test
    teardownWallet(sender);
    teardownWallet(receiver);
  }

  @Test
  void testWalletInvalidDestinationTransaction() {
    final Wallet sender = assertDoesNotThrow(() -> this.walletService.createWallet(100_00),
        "assertion on account creation failed");
    // give some random account id and private key. Deosn't actually matter as we want it to fail
    final Wallet receiver =
        this.walletService.createWallet("0.0.9999999", PrivateKey.generateECDSA().toString());

    TransferResponse response =
        assertDoesNotThrow(() -> this.walletService.transferBalance(sender, receiver, 10.2f));
    assertEquals(TransferResponse.UNKNOWN_ACCOUNT, response,
        "The transfer execpted the UNKNOWN_ACCOUNT response");

    // now let's try with a deleted account
    final Wallet receiver2 =
        this.walletService.createWallet("0.0.8187658", PrivateKey.generateECDSA().toString());
    response =
        assertDoesNotThrow(() -> this.walletService.transferBalance(sender, receiver, 10.2f));
    assertEquals(TransferResponse.UNKNOWN_ACCOUNT, response,
        "The transfer execpted the UNKNOWN_ACCOUNT response");

    teardownWallet(sender);
  }

  @Test
  void testWalletAccountNotAssociatedTransaction() {
    final Wallet sender = assertDoesNotThrow(() -> this.walletService.createWallet(100_00));
    try (final ClientPair clientPair = spinTestAccount(0)) {
      final Wallet receiver = this.walletService.createWallet(clientPair.accountId().toString(),
          clientPair.privateKey().toString());

      TransferResponse response =
          assertDoesNotThrow(() -> this.walletService.transferBalance(sender, receiver, 10.2f));
      assertEquals(TransferResponse.NOT_ACCEPTING_TOKEN, response,
          "The transfer execpted the NOT_ACCEPTING_TOKEN response");

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    teardownWallet(sender);
  }

  @Test
  void testWalletInsufficientBalance() {
    final Wallet sender = assertDoesNotThrow(() -> this.walletService.createWallet(1_00),
        "assertion on account creation failed");
    final Wallet receiver = assertDoesNotThrow(() -> this.walletService.createWallet(1_00),
        "assertion on account creation failed");

    final TransferResponse response =
        assertDoesNotThrow(() -> this.walletService.transferBalance(sender, receiver, 100.23f),
            "transaction threw when it should not have");
    assertEquals(TransferResponse.INSUFFICIENT_BALANCE, response,
        "The response should be INSUFFICIENT_BALANCE, but was not");

    teardownWallet(sender);
    teardownWallet(receiver);
  }

  @Test
  void testWalletTransferArgumentValidation() {
    final Wallet sender =
        this.walletService.createWallet("0.0.1111111", PrivateKey.generateECDSA().toString());
    final Wallet receiver =
        this.walletService.createWallet("0.0.9999999", PrivateKey.generateECDSA().toString());

    // top level nullability and sameness
    assertThrows(IllegalArgumentException.class,
        () -> this.walletService.transferBalance(receiver, receiver, 10f),
        "Should not be able to transfer currency between the same account");
    assertThrows(IllegalArgumentException.class,
        () -> this.walletService.transferBalance(null, receiver, 10f),
        "Should not be able to send from null wallet");
    assertThrows(IllegalArgumentException.class,
        () -> this.walletService.transferBalance(sender, null, 10f),
        "Should not be able to transfer to null wallet");
    assertThrows(IllegalArgumentException.class,
        () -> this.walletService.transferBalance(null, null, 10f),
        "Null wallets are not allowed");

    // number validation
    assertThrows(IllegalArgumentException.class,
        () -> this.walletService.transferBalance(sender, receiver, 0.0f),
        "Should not be able to transfer 0 tokens");
    assertThrows(IllegalArgumentException.class,
        () -> this.walletService.transferBalance(sender, receiver, -1.0f),
        "Should not be able to transfer negative tokens");
    assertThrows(IllegalArgumentException.class,
        () -> this.walletService.transferBalance(sender, receiver, 10.123f),
        "Should not be able to transfer amount of more than 2 decimals");
  }

  private void teardownWallet(Wallet wallet) {
    // now let's try to delete the re-init wallet
    AccountId accountId = AccountId.fromString(wallet.getWalletId());
    PrivateKey key = PrivateKey.fromStringECDSA(wallet.getWalletPrivateKey());
    // now we close like before by adding our data back into a ClientPair which is AutoClosable
    ClientPair pair = new ClientPair(accountId, key, this.operatorId, this.client);
    // this close wipes everything from this account and sends it back to our guarantor
    assertDoesNotThrow(pair::close,
        "There was a failure closing the pair that was constructed through a re-init wallet");
  }
}
