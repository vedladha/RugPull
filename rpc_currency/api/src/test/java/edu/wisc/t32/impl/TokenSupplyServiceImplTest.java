package edu.wisc.t32.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.math.BigDecimal;
import java.text.DecimalFormat;
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
  private TokenSupplyService supplyService;
  private WalletService walletService;

  @BeforeAll
  void setupResources() {
    tokenA = spinTestToken(100000_00);

    supplyService = assertDoesNotThrow(
        () -> TokenSupplyServiceImpl.create(this.operatorId.toString(),
            this.operatorKey.toString(), tokenA.tokenId().toString(),
            tokenA.adminKey().toString()),
        "An exception occurred while creating the token supply service");

    walletService = assertDoesNotThrow(
        () -> WalletService.getService(this.operatorId.toString(), this.operatorKey.toString(),
            tokenA.tokenId().toString()),
        "An exception occurred while creating the helper wallet service");
  }

  @AfterAll
  void teardownResources() {
    try {
      walletService.close();
      tokenA.close();
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
    // note we have to use big decimal since float is so imprecise
    // we should now validate that the supply has grown
    final TokenSupplyServiceImpl impl = (TokenSupplyServiceImpl) this.supplyService;
    final AccountId account = impl.getTreasury();
    // null key we only need balance, which doesn't require signing
    final WalletImpl wallet = new WalletImpl(account, null);

    final BigDecimal balanceBefore =
        new BigDecimal(Float.toString(this.walletService.getBalance(wallet)));
    assertDoesNotThrow(() -> this.supplyService.mint(123.45f),
        "Minting a valid amount of tokens should succeed");
    // minting might take a second
    assertDoesNotThrow(() -> Thread.sleep(4000));
    final BigDecimal balanceAfter =
        new BigDecimal(Float.toString(this.walletService.getBalance(wallet)));
    assertEquals(new BigDecimal("123.45"), balanceAfter.subtract(balanceBefore));
  }

  @Test
  void testMintInvalidArguments() {
    assertThrows(IllegalArgumentException.class, () -> this.supplyService.mint(0.0f),
        "Minting 0 tokens should throw an exception");

    assertThrows(IllegalArgumentException.class, () -> this.supplyService.mint(-50.0f),
        "Minting negative tokens should throw an exception");
  }

  // from WalletServiceImplTest
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
