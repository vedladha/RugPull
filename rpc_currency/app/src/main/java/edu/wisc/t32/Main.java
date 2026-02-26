package edu.wisc.t32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hedera.hashgraph.sdk.*;

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
  public static void main(String[] args) {
        operatorId = AccountId.fromString(System.getenv("OPERATOR_ID"));
        operatorKey = PrivateKey.fromStringECDSA(System.getenv("OPERATOR_KEY"));

        Client client = Client.forTestnet().setOperator(operatorId, operatorKey);
        
        PrivateKey adminKey = PrivateKey.generateED25519();
        
        TokenCreateTransaction transaction = new TokenCreateTransaction()
            .setTokenName("Rug Pull Coin")
            .setTokenSymbol("RPC")
            .setDecimals(2)
            .setInitialSupply(10000)
            .setTreasuryAccountId(operatorId)
            .setAdminKey(adminKey.getPublicKey())
            .freezeWith(client);

  }

}
