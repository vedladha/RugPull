package edu.wisc.t32.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import edu.wisc.t32.AbstractCryptoTest;
import edu.wisc.t32.api.TokenSupplyService;
import edu.wisc.t32.api.Wallet;
import edu.wisc.t32.api.WalletService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class TokenSupplyServiceImplTest extends AbstractCryptoTest {

  private TestToken tokenA;
  private TestToken tokenB;
  private TokenSupplyService supplyService;
  private WalletService walletService;

  @BeforeAll
  void setupResources() throws InterruptedException {
    tokenA = spinTestToken(100000_00);
    tokenB = spinTestToken(100000_00);

    supplyService = assertDoesNotThrow(
        () -> TokenSupplyServiceImpl.create(this.operatorId.toString(),
            this.operatorKey.toString(), tokenA.tokenId().toString(),
            tokenA.adminKey().toString()),
        "An exception occurred while creating the token supply service");

    walletService = assertDoesNotThrow(
        () -> WalletService.getService(this.operatorId.toString(), this.operatorKey.toString(),
            tokenA.tokenId().toString()),
        "An exception occurred while creating the helper wallet service");

    // this is really dumb, but we need to let the testnet update before we query account balances.
    Thread.sleep(4000);
  }

  @AfterAll
  void teardownResources() {
    try {
      walletService.close();
      tokenA.close();
      tokenB.close();
    } catch (Exception e) {
      throw new RuntimeException("Failed to cleanly teardown resources", e);
    }
  }

  @Test
  void testCreateServiceArgumentValidation() {
    assertThrows(IllegalArgumentException.class,
        () -> TokenSupplyServiceImpl.create(null, this.operatorKey.toString(),
            tokenA.tokenId().toString(), tokenA.adminKey().toString()),
        "Should not be able to create service with null operatorId");

    assertThrows(IllegalArgumentException.class,
        () -> TokenSupplyServiceImpl.create(this.operatorId.toString(), null,
            tokenA.tokenId().toString(), tokenA.adminKey().toString()),
        "Should not be able to create service with null operatorKey");
  }

  @Test
  void testMintValidAmount() {
    assertDoesNotThrow(() -> this.supplyService.mint(100.50f),
        "Minting a valid amount of tokens should succeed");
  }

  @Test
  void testMintInvalidArguments() {
    assertThrows(IllegalArgumentException.class, () -> this.supplyService.mint(0.0f),
        "Minting 0 tokens should throw an exception");

    assertThrows(IllegalArgumentException.class, () -> this.supplyService.mint(-50.0f),
        "Minting negative tokens should throw an exception");
  }

  @Test
  void testExchangeHbarValid() {
    // 1. Create a wallet (Starts with 0 HBAR)
    final Wallet wallet = assertDoesNotThrow(() -> this.walletService.createWallet(0),
        "Helper wallet creation should succeed");

    // 2. Fund the wallet with 10 HBAR so it has money to swap
    assertDoesNotThrow(() -> fundWalletWithHbar(wallet, 10),
        "Funding the test wallet with HBAR should succeed");

    final float hbarToSwap = 5.0f;

    // 3. Execute the swap (User gives 5 HBAR, receives Token A)
    final float receivedTokens = assertDoesNotThrow(
        () -> this.supplyService.exchangeHbar(wallet, hbarToSwap),
        "Swapping HBAR for Token A should succeed without network errors");

    // 4. Verify the AMM yielded a positive amount of tokens
    assertTrue(receivedTokens > 0.0f,
        "The AMM should return a positive amount of swapped tokens");

    teardownWallet(wallet);
  }

  /**
   * Helper method to send native HBAR from the operator to a test wallet.
   */
  private void fundWalletWithHbar(Wallet wallet, long hbarAmount) throws Exception {
    AccountId accountId = AccountId.fromString(wallet.getWalletId());

    TransactionResponse response = new TransferTransaction()
        .addHbarTransfer(this.client.getOperatorAccountId(),
            Hbar.from(-hbarAmount, com.hedera.hashgraph.sdk.HbarUnit.HBAR))
        .addHbarTransfer(accountId, Hbar.from(hbarAmount, com.hedera.hashgraph.sdk.HbarUnit.HBAR))
        .freezeWith(this.client)
        // Only the operator needs to sign when sending from the operator account
        .execute(this.client);

    response.getReceipt(this.client);
  }

  @Test
  void testExchangeHbarInvalidArguments() {
    final Wallet validWallet = new WalletImpl(this.operatorId, this.operatorKey);

    assertThrows(IllegalArgumentException.class,
        () -> this.supplyService.exchangeHbar(null, 10.0f),
        "Should not be able to swap HBAR with a null wallet");

    assertThrows(IllegalArgumentException.class,
        () -> this.supplyService.exchangeHbar(validWallet, 0.0f),
        "Should not be able to swap 0 HBAR");

    assertThrows(IllegalArgumentException.class,
        () -> this.supplyService.exchangeHbar(validWallet, -5.0f),
        "Should not be able to swap negative HBAR");
  }

  @Test
  void testExchangeTokenValid() {
    // 1. Create a wallet funded with Token A (via WalletService)
    final Wallet wallet = assertDoesNotThrow(() -> this.walletService.createWallet(100_00),
        "Helper wallet creation should succeed");

    // 2. We are going to swap Token A for Token B.
    // However, the wallet needs to be formally associated with Token B to receive it.
    assertDoesNotThrow(() -> associateWalletWithToken(wallet, tokenB.tokenId().toString()),
        "Associating the wallet with Token B should not throw");

    // We also need a supply service explicitly built for Token B to handle the swap logic
    final TokenSupplyService tokenBService = TokenSupplyServiceImpl.create(
        this.operatorId.toString(), this.operatorKey.toString(),
        tokenB.tokenId().toString(), tokenB.adminKey().toString());

    final float tokenAToSwap = 10.0f;

    // 3. Execute the swap (User gives Token A, receives Token B)
    final float receivedTokens = assertDoesNotThrow(
        () -> tokenBService.exchange(wallet, tokenA.tokenId().toString(), tokenAToSwap),
        "Swapping Token A for Token B should succeed without network errors");

    assertTrue(receivedTokens > 0.0f,
        "The AMM should return a positive amount of swapped Token B");

    teardownWallet(wallet);
  }

  @Test
  void testExchangeTokenInvalidArguments() {
    final Wallet validWallet = new WalletImpl(this.operatorId, this.operatorKey);

    assertThrows(IllegalArgumentException.class,
        () -> this.supplyService.exchange(null, tokenA.tokenId().toString(), 10.0f),
        "Should not be able to swap with a null wallet");

    assertThrows(IllegalArgumentException.class,
        () -> this.supplyService.exchange(validWallet, null, 10.0f),
        "Should not be able to swap a null token ID");

    assertThrows(IllegalArgumentException.class,
        () -> this.supplyService.exchange(validWallet, tokenA.tokenId().toString(), 0.0f),
        "Should not be able to swap 0 tokens");
  }

  // --- Helper Methods ---

  private void teardownWallet(Wallet wallet) {
    AccountId accountId = AccountId.fromString(wallet.getWalletId());
    PrivateKey key = PrivateKey.fromStringECDSA(wallet.getWalletPrivateKey());
    ClientPair pair = new ClientPair(accountId, key, this.operatorId, this.client);
    assertDoesNotThrow(pair::close, "Failed to teardown wallet");
  }

  /**
   * Helper method to associate a test wallet with a specific token.
   * Required before a wallet can receive a new type of token from an exchange.
   */
  private void associateWalletWithToken(Wallet wallet, String tokenIdStr) throws Exception {
    AccountId accountId = AccountId.fromString(wallet.getWalletId());
    PrivateKey key = PrivateKey.fromStringECDSA(wallet.getWalletPrivateKey());

    TokenAssociateTransaction tx = new TokenAssociateTransaction()
        .setAccountId(accountId)
        .setTokenIds(java.util.List.of(com.hedera.hashgraph.sdk.TokenId.fromString(tokenIdStr)))
        .freezeWith(this.client)
        .sign(key);

    TransactionResponse response = tx.execute(this.client);
    response.getReceipt(this.client);
  }
}
