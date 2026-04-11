package edu.wisc.t32.controller;

import edu.wisc.t32.dto.DailyRewardStatusRequest;
import edu.wisc.t32.model.DailyReward;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.DailyRewardRepository;
import edu.wisc.t32.repository.UserWalletRepository;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.RpcWalletService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling user daily rewards.
 */
@RestController
@RequestMapping("/daily")
public class DailyRewardController {
  private static final Logger LOGGER = LoggerFactory.getLogger(DailyRewardController.class);
  private static final ZoneId UTC = ZoneId.of("UTC");

  private final CurrentUserService currentUserService;
  private final UserWalletRepository userWalletRepository;
  private final DailyRewardRepository dailyRewardRepository;
  private final RpcWalletService walletService;

  /**
   * Daily reward controller initialization.
   *
   * @param currentUserService    the current user service
   * @param dailyRewardRepository the daily reward repository
   */
  public DailyRewardController(CurrentUserService currentUserService,
                               UserWalletRepository userWalletRepository,
                               DailyRewardRepository dailyRewardRepository,
                               RpcWalletService walletService) {
    this.currentUserService = currentUserService;
    this.userWalletRepository = userWalletRepository;
    this.dailyRewardRepository = dailyRewardRepository;
    this.walletService = walletService;
  }

  /**
   * Gets the claim status of the daily reward for a user.
   *
   * @param token user's jwt token
   * @return this response entity
   */
  @GetMapping
  public ResponseEntity<?> getDailyRewardStatus(
      @CookieValue(value = "jwt", required = false) String token) {
    final Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error",
          "Authentication required"));
    }

    final int id = currentUser.get().getUserId();
    final DailyRewardStatusRequest response = DailyRewardStatusRequest.next();
    // if the user has no daily reward entry return false it can't have been claimed
    boolean reward = dailyRewardRepository.findById(id)
        .map(DailyReward::getClaimedLast)
        .map((lastClaimed) ->
            ChronoUnit.HOURS.between(lastClaimed, LocalDateTime.now(UTC)) < 24)
        .orElse(false);
    response.setClaimed(reward);

    return ResponseEntity.ok(Map.of("status", response));
  }

  /**
   * Claims the user's daily reward if not already claimed.
   *
   * @param token the user's jwt token
   * @return the repsonse entity
   */
  @GetMapping("/claim")
  public ResponseEntity<?> claimDailyReward(
      @CookieValue(value = "jwt", required = false) String token) {
    final Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error",
          "Authentication required"));
    }

    final int id = currentUser.get().getUserId();

    final LocalDateTime now = LocalDateTime.now(UTC);
    final DailyReward reward = dailyRewardRepository.findById(id).orElseGet(() -> {
      final DailyReward newReward = new DailyReward();
      newReward.setUserId(id);
      newReward.setClaimedLast(now.minusDays(1));
      return newReward;
    });

    final long hoursElapsed = ChronoUnit.HOURS.between(reward.getClaimedLast(), now);
    if (hoursElapsed < 24) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "Daily reward has already been claimed today"));
    }

    final Optional<UserWallet> wallet = userWalletRepository.findUserWalletByUserId(id);
    if (wallet.isEmpty()) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Internal service error fetching wallet"));
    }

    try {
      // 10.0f hardcoded for now in the very very near future we should discuss what to do for this.
      // streaks? etc easy to append to the database.
      walletService.fundAccount(wallet.get(), 10.0f);
      reward.setClaimedLast(now);
      dailyRewardRepository.save(reward);
    } catch (IllegalStateException e) {
      LOGGER.error("Error occurred while funding account with reward", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Internal service error claiming reward"));
    }

    return ResponseEntity.status(HttpStatus.OK).body(null);
  }
}
