package edu.wisc.t32.services;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import edu.wisc.t32.api.TokenSupplyService;
import edu.wisc.t32.api.TransferResponse;
import edu.wisc.t32.api.Wallet;
import edu.wisc.t32.api.WalletService;
import edu.wisc.t32.impl.WalletImpl;
import edu.wisc.t32.model.UserWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for creating and managing remote procedure call (RPC) wallets.
 *
 * <p>This service interacts with the external {@code WalletService} API to provision
 * new digital wallets and supply them with initial funding based on application properties.
 */
@Service
public class RpcWalletService {
  private static final Logger LOGGER = LoggerFactory.getLogger(RpcWalletService.class);

  @Value("${wallet.operator.id:}")
  private String operatorId;

  @Value("${wallet.operator.key:}")
  private String operatorKey;

  @Value("${wallet.currency.id:}")
  private String currencyId;

  @Value("${wallet.currency.supply.key}")
  private String currencySupplyKey;

  @Value("${wallet.new-account.initial-funding:10}")
  private int initialFunding;

  /**
   * Funds a wallet account by minting new tokens of amount rewardAmount then depositing it into the
   * user account.
   *
   * @param userWallet   the user wallet to fund
   * @param rewardAmount the reward to fund the account with
   * @throws IllegalStateException thrown if the wallet is failed to be funded for some reason
   */
  public void fundAccount(UserWallet userWallet, float rewardAmount) throws IllegalStateException {
    if (userWallet == null) {
      throw new IllegalArgumentException("null user wallet is not valid for fund account");
    }

    try (WalletService walletService = WalletService.getService(operatorId, operatorKey,
        currencyId)) {
      try (TokenSupplyService supplyService = TokenSupplyService.getService(operatorId, operatorKey,
          currencyId, currencySupplyKey)) {
        supplyService.mint(rewardAmount); // mint's then halts until tokens can be minted
      }

      final Wallet wallet = walletService.createWallet(userWallet.getWalletAddress(),
          userWallet.getWalletPrivateKey());
      // funds the wallets with the newly minted tokens.
      walletService.fundWallet(wallet, rewardAmount);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to fund wallet: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a new funded wallet using the configured operator credentials.
   *
   * <p>Validates that all required application properties are present before attempting
   * to communicate with the external wallet service.
   *
   * @return a {@link WalletCredentials} record containing the new wallet's ID and private key
   * @throws IllegalStateException if required configuration properties are missing, or if
   *                               the external wallet service fails to return a valid wallet
   */
  public WalletCredentials createWallet() {
    if (operatorId == null || operatorId.isBlank()) {
      throw new IllegalStateException("Wallet operator ID is missing. Set wallet.operator.id.");
    }
    if (operatorKey == null || operatorKey.isBlank()) {
      throw new IllegalStateException(
          "Wallet operator key is missing. Set wallet.operator.key.");
    }
    if (currencyId == null || currencyId.isBlank()) {
      throw new IllegalStateException("Wallet currency ID is missing. Set wallet.currency.id.");
    }

    try (WalletService walletService =
             WalletService.getService(operatorId, operatorKey, currencyId)) {
      Wallet wallet = walletService.createWallet(Math.max(0, initialFunding));
      if (wallet == null || wallet.getWalletId() == null || wallet.getWalletId().isBlank()) {
        throw new IllegalStateException(
            "Wallet service did not return a valid wallet ID for the new user.");
      }
      if (wallet.getWalletPrivateKey() == null || wallet.getWalletPrivateKey().isBlank()) {
        throw new IllegalStateException(
            "Wallet service did not return a private key for the new user.");
      }
      return new WalletCredentials(wallet.getWalletId(), wallet.getWalletPrivateKey());
    } catch (IllegalStateException | IllegalArgumentException e) {
      throw new IllegalStateException(
          "Failed to create wallet: "
              + e.getClass().getSimpleName()
              + ": "
              + e.getMessage(),
          e);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create wallet: " + e.getMessage(), e);
    }
  }

  /**
   * Retrieves the current balance for the specified user wallet.
   *
   * <p>This method initializes a {@link WalletService} using the configured operator
   * credentials and currency ID, connects to the specified user wallet, and fetches
   * its current balance.
   *
   * @param userWallet the {@link UserWallet} containing the target wallet's address and private key
   * @return the current balance of the wallet as a {@code float}
   * @throws IllegalStateException if the balance cannot be fetched due to invalid arguments,
   *                               state issues, or underlying network/service exceptions
   */
  public float getWalletBalance(UserWallet userWallet) {
    try (WalletService walletService = WalletService.getService(operatorId, operatorKey,
        currencyId)) {
      Wallet wallet =
          walletService.createWallet(userWallet.getWalletAddress(),
              userWallet.getWalletPrivateKey());

      return walletService.getBalance(wallet);
    } catch (IllegalStateException | IllegalArgumentException e) {
      throw new IllegalStateException("Failed to fetch wallet balance: "
          + e.getClass().getSimpleName()
          + ": "
          + e.getMessage());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to fetch wallet balance: " + e.getMessage(), e);
    }
  }

  /**
   * Transfers RPC from the user's wallet to the operator wallet.
   *
   * @param userWallet the user wallet sending RPC
   * @param amount the amount of RPC to transfer
   */
  public void transferToOperator(UserWallet userWallet, float amount) {
    transferBetweenUserAndOperator(userWallet, amount, true);
  }

  /**
   * Transfers RPC from the operator wallet to the user's wallet.
   *
   * @param userWallet the user wallet receiving RPC
   * @param amount the amount of RPC to transfer
   */
  public void transferFromOperator(UserWallet userWallet, float amount) {
    transferBetweenUserAndOperator(userWallet, amount, false);
  }

  private void transferBetweenUserAndOperator(
      UserWallet userWallet,
      float amount,
      boolean toOperator) {
    if (userWallet == null) {
      throw new IllegalArgumentException("null user wallet is not valid for transfer");
    }

    try (WalletService walletService = WalletService.getService(operatorId, operatorKey,
        currencyId)) {
      Wallet operatorWallet = buildOperatorWallet();
      Wallet userRpcWallet = walletService.createWallet(
          userWallet.getWalletAddress(),
          userWallet.getWalletPrivateKey()
      );

      Wallet sender = toOperator ? userRpcWallet : operatorWallet;
      Wallet receiver = toOperator ? operatorWallet : userRpcWallet;

      TransferResponse response = walletService.transferBalance(sender, receiver, amount);
      if (response == TransferResponse.SUCCESS) {
        return;
      }
      if (response == TransferResponse.INSUFFICIENT_BALANCE) {
        throw new IllegalArgumentException("Insufficient balance");
      }
      throw new IllegalStateException("Failed to transfer balance: " + response.name());
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to transfer balance: " + e.getMessage(), e);
    }
  }

  private Wallet buildOperatorWallet() {
    return new WalletImpl(
        AccountId.fromString(operatorId),
        PrivateKey.fromStringECDSA(operatorKey)
    );
  }

  /**
   * A container for the generated wallet credentials.
   *
   * @param walletId         the public identifier of the created wallet
   * @param walletPrivateKey the private key associated with the created wallet
   */
  public record WalletCredentials(String walletId, String walletPrivateKey) {
  }
}
