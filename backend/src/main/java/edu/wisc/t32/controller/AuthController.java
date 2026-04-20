package edu.wisc.t32.controller;

import edu.wisc.t32.dto.PasswordChangeRequest;
import edu.wisc.t32.dto.UserRegisteredEvent;
import edu.wisc.t32.model.User;
import edu.wisc.t32.services.AuthService;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling user authentication and registration.
 *
 * <p>TODO: Basic JWT flow is in place. Next, use it to protect routes.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
  private static final String JWT_COOKIE_NAME = "jwt";
  private static final long JWT_COOKIE_MAX_AGE_SECONDS = 86400;

  AuthService authService;
  JwtUtil jwtUtil;
  CurrentUserService currentUserService;

  /**
   * Constructs the AuthController with required dependencies.
   *
   * @param authService           service for authentication logic
   * @param jwtUtil               utility for generating JWT tokens
   * @param currentUserService    service for resolving the authenticated user
   */
  public AuthController(AuthService authService, JwtUtil jwtUtil,
                        CurrentUserService currentUserService) {
    this.authService = authService;
    this.jwtUtil = jwtUtil;
    this.currentUserService = currentUserService;
  }

  /**
   * Registers a new user and provisions a new wallet.
   *
   * @param body map containing displayName, email, and password
   * @return response containing the user's email and display name, or an error status
   */
  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody Map<String, String> body,
                                    HttpServletResponse response) {
    String displayName = body.get("displayName");
    String email = body.get("email");
    String password = body.get("password");

    try {
      UserRegisteredEvent user = authService.registerWithWallet(displayName, email, password);
      String token = jwtUtil.generateToken(user.email());

      response.addHeader(HttpHeaders.SET_COOKIE,
          buildJwtCookie(token, JWT_COOKIE_MAX_AGE_SECONDS).toString());

      return ResponseEntity.ok(
          Map.of("email", user.email(), "displayName", user.userProfile().getDisplayName()));
    } catch (edu.wisc.t32.exception.WalletProvisioningException e) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of(
              "error", e.getMessage(),
              "details", e.getCause() != null ? e.getCause().getMessage() : "No details"
          ));
    }
  }

  /**
   * Authenticates a user and will eventually set a JWT cookie.
   *
   * @param body     map containing the user's email and password
   * @param response the HTTP response used to attach authentication cookies
   * @return response containing the user's email and display name, or an unauthorized error
   */
  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                 HttpServletResponse response) {
    String email = body.get("email");
    String password = body.get("password");
    try {
      User user = authService.login(email, password);
      String token = jwtUtil.generateToken(user.getEmail());

      response.addHeader(HttpHeaders.SET_COOKIE,
          buildJwtCookie(token, JWT_COOKIE_MAX_AGE_SECONDS).toString());

      return ResponseEntity.ok(Map.of(
          "email", user.getEmail(),
          "displayName", user.getUserProfile().getDisplayName()));
    } catch (RuntimeException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Retrieves the authenticated user's profile information.
   *
   * @param token the JWT token extracted from the HTTP-only cookie
   * @return the user's profile information when authenticated
   */
  @GetMapping("/profile")
  public ResponseEntity<?> getProfile(@CookieValue(name = "jwt", required = false) String token) {
    return currentUserService.getAuthenticatedUser(token)
        .<ResponseEntity<?>>map(user -> ResponseEntity.ok(Map.of(
            "user", Map.of(
                "email", user.getEmail(),
                "displayName", user.getUserProfile().getDisplayName()))))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Authentication required")));
  }

  /**
   * Logs out the user by clearing their authentication cookie.
   *
   * @param response the HTTP response used to clear the JWT cookie
   * @return a success message
   */
  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, buildJwtCookie("", 0).toString());

    return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
  }

  /**
   * Changes the password for the currently authenticated user.
   *
   * @param token the JWT token extracted from the HTTP-only cookie
   * @param request the password-change request containing the current and new password
   * @return a success response when the password is updated
   */
  @PutMapping("/password")
  public ResponseEntity<?> changePassword(
      @CookieValue(name = "jwt", required = false) String token,
      @RequestBody PasswordChangeRequest request) {
    return currentUserService.getAuthenticatedUser(token)
        .<ResponseEntity<?>>map(user -> {
          authService.changePassword(user, request.getCurrentPassword(), request.getNewPassword());
          return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        })
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Authentication required")));
  }

  private ResponseCookie buildJwtCookie(String token, long maxAgeSeconds) {
    return ResponseCookie.from(JWT_COOKIE_NAME, token)
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(maxAgeSeconds)
        .build();
  }
}
