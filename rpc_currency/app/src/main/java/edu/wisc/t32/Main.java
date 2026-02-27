package edu.wisc.t32;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import java.nio.file.Files;
import java.nio.file.Path;
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

  /**
   * Launches the $RPC deployment application.
   *
   * @param args the command line arguments given to the $RPC deployment
   */
    @SuppressWarnings("null")
    public static void main(String[] args) {
        AccountId operatorId = AccountId.fromString(System.getenv("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromStringECDSA(System.getenv("OPERATOR_KEY"));

        try (Client client = Client.forTestnet().setOperator(operatorId, operatorKey)) {
            // adminKey has full control over token configuration and properties. It allows updating token name, 
            // symbol, treasury, keys, and other properties. If not set, the token becomes immutable.
            PrivateKey adminKey = PrivateKey.generateED25519();
            // supplyKey controls minting (increasing supply) and burning (decreasing supply) of tokens. If not 
            // set, no minting or burning are possible.
            PrivateKey supplyKey = PrivateKey.generateED25519();
            
            Path keyFile = Path.of("rpc_keys.txt").toAbsolutePath();
            Files.writeString(
                keyFile,
                "adminKey=" + adminKey + System.lineSeparator()
                    + "supplyKey=" + supplyKey + System.lineSeparator()
            );
            LOGGER.info("Saved adminKey and supplyKey to {}", keyFile);

            TokenCreateTransaction transaction = new TokenCreateTransaction()
                .setTokenName("Rug Pull Coin")
                .setTokenSymbol("RPC")
                .setDecimals(2)
                .setInitialSupply(10000)
                .setTreasuryAccountId(operatorId)
                .setAdminKey(adminKey.getPublicKey())
                .setSupplyKey(supplyKey.getPublicKey())
                .freezeWith(client);

            TransactionResponse txResponse = transaction.sign(adminKey).execute(client);
            TransactionReceipt receipt = txResponse.getReceipt(client);
            LOGGER.info("Fungible token created: {}", receipt.tokenId);
        } 
        catch (Exception e) {
            LOGGER.error("Token creation failed.", e);
        } 
    }
}
