package edu.wisc.t32;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenDeleteTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test file for all methods in the main class.
 */
public class MainTest extends AbstractCryptoTest {

  @TempDir
  Path tempDir;

  @Test
  public void testGetEnvOrThrow() {
    assertDoesNotThrow(() -> Main.getenvOrThrow(Main.OPERATOR_ID_ENV),
        "get env or throw throws on present variable");
    assertDoesNotThrow(() -> Main.getenvOrThrow(Main.OPERATOR_KEY_ENV),
        "get env or throw throws on present variable");
    assertThrows(IllegalArgumentException.class,
        () -> Main.getenvOrThrow(UUID.randomUUID().toString()),
        "getEnvOrThrow does not throw when accessing environment variable that is not present");
  }

  @Test
  public void testCreateCurrency() {
    final Path keysFile = tempDir.resolve("keys.txt");
    final Main.CryptoCreationResult result =
        assertDoesNotThrow(() -> Main.createCurrency(client, keysFile),
            "createCurrency throws when it should not.");
    System.out.println(result.tokenId());
    System.out.flush();

    assertEquals(Status.SUCCESS, result.status(), "return status must be okay");
    assertNotNull(result.tokenId(), "token id must not be null in result");
    assertNotNull(result.adminKey(), "Admin key must not be null in result");
    assertNotNull(result.supplyKey(), "Supply key must not be null in result");

    // here we delete our tokens before testing any outputs so our asserts
    // don't leave before we clean up
    TokenDeleteTransaction transaction =
        new TokenDeleteTransaction().setTokenId(result.tokenId()).freezeWith(client)
            .sign(result.adminKey());
    final TransactionResponse response = assertDoesNotThrow(() -> transaction.execute(client),
        "Deleting created currency should not fail");
    final TransactionReceipt receipt = assertDoesNotThrow(() -> response.getReceipt(client),
        "getting the receipt of a valid tranasction should not throw");
    assertEquals(Status.SUCCESS, receipt.status, "token deletion transaction should succeeed");

    List<String> lines = assertDoesNotThrow(() -> Files.readAllLines(keysFile));
    assertEquals("tokenId=%s".formatted(result.tokenId()), lines.get(0));
    assertEquals("adminKey=%s".formatted(result.adminKey()), lines.get(1));
    assertEquals("supplyKey=%s".formatted(result.supplyKey()), lines.get(2));
  }
}
