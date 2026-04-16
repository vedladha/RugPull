package edu.wisc.t32.controller;

import edu.wisc.t32.dto.RouletteSpinRequest;
import edu.wisc.t32.dto.RouletteSpinResponse;
import edu.wisc.t32.model.User;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.RouletteService;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the earn-page roulette game.
 */
@RestController
@RequestMapping("/roulette")
public class RouletteController {
  private final CurrentUserService currentUserService;
  private final RouletteService rouletteService;

  /**
   * Creates the controller with the dependencies needed to resolve users and spin roulette.
   *
   * @param currentUserService service used to resolve the authenticated user
   * @param rouletteService service used to execute the roulette wager
   */
  public RouletteController(
      CurrentUserService currentUserService,
      RouletteService rouletteService) {
    this.currentUserService = currentUserService;
    this.rouletteService = rouletteService;
  }

  /**
   * Spins the roulette wheel for the authenticated user.
   *
   * @param token the JWT token extracted from the HTTP-only cookie
   * @param request the spin request containing wager and bet selection
   * @return the spin result or an error response when validation/authentication fails
   */
  @PostMapping("/spin")
  public ResponseEntity<?> spin(
      @CookieValue(name = "jwt", required = false) String token,
      @RequestBody RouletteSpinRequest request) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    try {
      RouletteSpinResponse response = rouletteService.spin(
          currentUser.get(),
          request.getWager(),
          request.getBetType(),
          request.getBetValue()
      );
      return ResponseEntity.ok(Map.of("spin", response));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
