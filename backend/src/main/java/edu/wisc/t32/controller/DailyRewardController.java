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

  // Streak configuration constants
  private static final long STREAK_GRACE_PERIOD_HOURS = 48;
  private static final float BASE_REWARD = 10.0f;

  private final CurrentUserService currentUserService;
  private final UserWalletRepository userWalletRepository;
  private final DailyRewardRepository dailyRewardRepository;
  private final RpcWalletService walletService;

  /**
   * represents a new daily reward controller.
   *
   * @param currentUserService    the current user service
   * @param userWalletRepository  the wallet repository
   * @param dailyRewardRepository the daily reward repository
   * @param walletService         and the wallet service
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
   * Calculates the reward amount based on the user's current streak.
   * Example: 10 base + 2 bonus for every 10 streak days.
   */
  private float calculateReward(int streak) {
    return BASE_REWARD + ((streak / 10) * 2.0f);
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

    dailyRewardRepository.findById(id).ifPresentOrElse(reward -> {
      long hoursElapsed = ChronoUnit.HOURS.between(reward.getClaimedLast(), LocalDateTime.now(UTC));
      boolean claimed = hoursElapsed < 24;

      // Determine what their streak WILL be next time they claim
      int activeStreak = reward.getStreak();
      if (hoursElapsed > STREAK_GRACE_PERIOD_HOURS) {
        activeStreak = 0; // Streak was lost
      }

      response.setClaimed(claimed);
      response.setStreak(reward.getStreak()); // Return actual current DB streak
      response.setNextReward(calculateReward(activeStreak + 1));
    }, () -> {
      // Brand new user
      response.setClaimed(false);
      response.setStreak(0);
      response.setNextReward(calculateReward(1));
    });

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
      newReward.setClaimedLast(now.minusDays(1)); // Ensures exactly 24h elapsed
      newReward.setStreak(0);
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

    if (hoursElapsed > STREAK_GRACE_PERIOD_HOURS) {
      reward.setStreak(1);
    } else {
      reward.setStreak(reward.getStreak() + 1);
    }

    float rewardAmount = calculateReward(reward.getStreak());

    try {
      walletService.fundAccount(wallet.get(), rewardAmount);
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
