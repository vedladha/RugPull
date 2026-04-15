package edu.wisc.t32.controller;

import edu.wisc.t32.dto.SlotSpinRequest;
import edu.wisc.t32.dto.SlotSpinResponse;
import edu.wisc.t32.model.User;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.SlotMachineService;
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
 * REST controller for the earn-page slot machine game.
 */
@RestController
@RequestMapping("/slots")
public class SlotMachineController {
  private final CurrentUserService currentUserService;
  private final SlotMachineService slotMachineService;

  /**
   * Creates the controller with the dependencies needed to resolve users and spin slots.
   *
   * @param currentUserService service used to resolve the authenticated user
   * @param slotMachineService service used to execute the slot-machine wager
   */
  public SlotMachineController(
      CurrentUserService currentUserService,
      SlotMachineService slotMachineService) {
    this.currentUserService = currentUserService;
    this.slotMachineService = slotMachineService;
  }

  /**
   * Spins the slot machine for the authenticated user.
   *
   * @param token the JWT token extracted from the HTTP-only cookie
   * @param request the spin request containing the wager amount
   * @return the spin result or an error response when validation/authentication fails
   */
  @PostMapping("/spin")
  public ResponseEntity<?> spin(
      @CookieValue(name = "jwt", required = false) String token,
      @RequestBody SlotSpinRequest request) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    try {
      SlotSpinResponse response = slotMachineService.spin(currentUser.get(), request.getWager());
      return ResponseEntity.ok(Map.of("spin", response));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
