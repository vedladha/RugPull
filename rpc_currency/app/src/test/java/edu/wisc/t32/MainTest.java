package edu.wisc.t32;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenDeleteTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test file for all methods in the main class.
 */
public class MainTest extends AbstractCryptoTest {

  @TempDir
  Path tempDir;

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

    TokenDeleteTransaction transaction =
        new TokenDeleteTransaction().setTokenId(result.tokenId()).freezeWith(client)
            .sign(result.adminKey());
    final TransactionResponse response = assertDoesNotThrow(() -> transaction.execute(client),
        "Deleting created currency should not fail");
    final TransactionReceipt receipt = assertDoesNotThrow(() -> response.getReceipt(client),
        "getting the receipt of a valid tranasction should not throw");
    assertEquals(Status.SUCCESS, receipt.status, "token deletion transaction should succeeed");


    List<String> lines = assertDoesNotThrow(() -> Files.readAllLines(keysFile));
    assertEquals("adminKey=%s".formatted(result.adminKey()), lines.get(0));
    assertEquals("supplyKey=%s".formatted(result.supplyKey()), lines.get(1));
  }
}
