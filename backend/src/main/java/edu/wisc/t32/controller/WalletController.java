package edu.wisc.t32.controller;

import edu.wisc.t32.dto.WalletFundRequest;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.UserWalletRepository;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.RpcWalletService;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing user wallets.
 *
 * <p>Provides endpoints for retrieving wallet information, such as the current balance,
 * for authenticated users.
 */
@RestController
@RequestMapping("/wallets")
public class WalletController {

  private final RpcWalletService walletService;
  private final CurrentUserService currentUserService;
  private final UserWalletRepository userWalletRepository;

  /**
   * Constructs a new {@code WalletController} with the required services and repositories.
   *
   * @param walletService        the service used to interact with the underlying wallet RPC
   * @param currentUserService   the service used to authenticate and retrieve the current user
   * @param userWalletRepository the repository for accessing user wallet records
   */
  public WalletController(RpcWalletService walletService, CurrentUserService currentUserService,
                          UserWalletRepository userWalletRepository) {
    this.walletService = walletService;
    this.currentUserService = currentUserService;
    this.userWalletRepository = userWalletRepository;
  }

  /**
   * Retrieves the wallet balance for the currently authenticated user.
   *
   * <p>This endpoint requires a valid JWT token provided via a cookie. It verifies the user's
   * identity, fetches their associated wallet from the database, and queries the RPC service
   * for the current balance.
   *
   * @param token the JWT authentication token extracted from the "jwt" cookie; may be null if
   *              missing
   * @return a {@link ResponseEntity} containing:
   *        <ul>
   *        <li>{@code 200 OK} with the balance (float) if successful</li>
   *        <li>{@code 401 UNAUTHORIZED} with an error map if the token is invalid or missing</li>
   *        <li>{@code 500 INTERNAL_SERVER_ERROR} with an error map if
   *            the user has no associated wallet</li>
   *        </ul>
   */
  @GetMapping
  public ResponseEntity<?> getWalletBalance(
      @CookieValue(name = "jwt", required = false) String token) {

    final Optional<User> user = currentUserService.getAuthenticatedUser(token);
    if (user.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    final Optional<UserWallet> wallet = userWalletRepository.findUserWalletByUserId(
        user.get().getUserId());
    if (wallet.isEmpty()) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Internal service error fetching wallet"));
    }

    float balance = this.walletService.getWalletBalance(wallet.get());
    return ResponseEntity.ok(balance);
  }

  /**
   * Funds the connected and authenticated wallet with the provided token amount.
   *
   * @param token the user's jwt token
   * @return a response entity.
   */
  @PostMapping("/fund")
  public ResponseEntity<?> fundWallet(@CookieValue(name = "jwt", required = false) String token,
                                      @RequestBody WalletFundRequest fundRequest) {
    final Optional<User> user = currentUserService.getAuthenticatedUser(token);
    if (user.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    final Optional<UserWallet> wallet = userWalletRepository.findUserWalletByUserId(
        user.get().getUserId());
    if (wallet.isEmpty()) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Internal service error fetching wallet"));
    }

    if (fundRequest.getAmount() <= 0) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "Can not fund account with 0 or less tokens"));
    }

    try {
      this.walletService.fundAccount(wallet.get(), fundRequest.getAmount());
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Internal service error while funding wallet"));
    }

    return ResponseEntity.ok().build();
  }
}
