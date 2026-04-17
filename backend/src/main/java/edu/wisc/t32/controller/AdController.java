package edu.wisc.t32.controller;

import edu.wisc.t32.dto.AdStartResponse;
import edu.wisc.t32.model.AdSession;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.AdSessionRepository;
import edu.wisc.t32.repository.UserWalletRepository;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.RpcWalletService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsible for managing server-authoritative ad sessions.
 * Ensures clients cannot spoof ad watch times by tracking initialization
 * and completion timestamps securely on the server.
 */
@RestController
@RequestMapping("/ads")
public class AdController {

  private record Ad(String title, int durationInSeconds, float reward, String videoUrl) {
  }

  private static final ZoneId UTC = ZoneId.of("UTC");

  private final List<Ad> adCatalog = List.of(
      new Ad(
          "Synapse Data Analytics",
          8,
          5.0f,
          "ads/synapse_ad.mp4"
      ),
      new Ad(
          "Smart Mug",
          8,
          5.0f,
          "ads/smartmug_ad.mp4"
      ),
      new Ad(
          "Aura Sound",
          8,
          5.0f,
          "ads/aura_sound_ad.mp4"
      ),
      new Ad(
          "Verdant Smart Garden",
          8,
          5.0f,
          "ads/smartgarden_ad.mp4"
      ),
      new Ad(
          "Lumina Pack",
          8,
          5.0f,
          "ads/luminapack_ad.mp4"
      )
  );

  private final CurrentUserService currentUserService;
  private final AdSessionRepository adSessionRepository;
  private final UserWalletRepository userWalletRepository;
  private final RpcWalletService walletService;

  /**
   * Creates a new AdController.
   *
   * @param currentUserService   the user service
   * @param adSessionRepository  ad session repository
   * @param userWalletRepository user wallet repository
   * @param walletService        wallet service
   */
  public AdController(CurrentUserService currentUserService,
                      AdSessionRepository adSessionRepository,
                      UserWalletRepository userWalletRepository,
                      RpcWalletService walletService) {
    this.currentUserService = currentUserService;
    this.adSessionRepository = adSessionRepository;
    this.userWalletRepository = userWalletRepository;
    this.walletService = walletService;
  }

  /**
   * Initializes a new ad session for the authenticated user.
   * Selects a random ad, logs the start time, and returns the viewing details to the client.
   *
   * @param token The user's JWT authentication cookie
   * @return 200 OK with session ID, video URL, duration, and reward amount
   */
  @GetMapping("/start")
  public ResponseEntity<?> startAd(@CookieValue(value = "jwt", required = false) String token) {
    Optional<User> userOpt = currentUserService.getAuthenticatedUser(token);
    if (userOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }
    User user = userOpt.get();

    // get an add from our catalog
    Ad selectedAd = adCatalog.get(new Random().nextInt(adCatalog.size()));

    AdSession session = new AdSession();
    session.setUserId(user.getUserId());
    session.setAdTitle(selectedAd.title);
    session.setRequiredDurationSeconds(selectedAd.durationInSeconds);
    session.setStartedAt(LocalDateTime.now(UTC));

    adSessionRepository.save(session);

    return ResponseEntity.ok(Map.of("session", AdStartResponse.fromSession(
        selectedAd.reward,
        selectedAd.videoUrl,
        session
    )));
  }

  /**
   * Validates an ad session and processes the reward payout.
   * Strictly enforces the required watch duration by comparing the current server time
   * against the session's initialization time.
   *
   * @param token   The user's JWT authentication cookie
   * @param payload JSON containing the sessionId
   * @return 200 OK if successful, 400/425 if spoofed or invalid
   */
  @PostMapping("/claim")
  public ResponseEntity<?> claimAdReward(@CookieValue(value = "jwt", required = false) String token,
                                         @RequestBody Map<String, String> payload) {

    Optional<User> userOpt = currentUserService.getAuthenticatedUser(token);
    if (userOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    String sessionId = payload.get("sessionId");
    if (sessionId == null || sessionId.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Session ID is missing."));
    }

    Optional<AdSession> sessionOpt = adSessionRepository.findById(sessionId);

    if (sessionOpt.isEmpty() || sessionOpt.get().isClaimed()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Invalid or already claimed session."));
    }

    AdSession session = sessionOpt.get();
    long secondsElapsed =
        ChronoUnit.SECONDS.between(session.getStartedAt(), LocalDateTime.now(UTC));

    if (secondsElapsed < (session.getRequiredDurationSeconds() - 2)) {
      return ResponseEntity.status(HttpStatus.TOO_EARLY)
          .body(Map.of("error", "Ad completion spoofing detected. Required duration not met."));
    }

    float rewardAmount = adCatalog.stream()
        .filter(ad -> ad.title.equals(session.getAdTitle()))
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Ad title in database does not match active catalog."))
        .reward;

    Optional<UserWallet> walletOpt =
        userWalletRepository.findUserWalletByUserId(session.getUserId());
    if (walletOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "User wallet not found for payout."));
    }

    try {
      walletService.fundAccount(walletOpt.get(), rewardAmount);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Blockchain funding transaction failed."));
    }

    session.setClaimed(true);
    adSessionRepository.save(session);
    return ResponseEntity.ok(Map.of(
        "message", "Reward claimed successfully!",
        "rewardAmount", rewardAmount
    ));
  }
}
