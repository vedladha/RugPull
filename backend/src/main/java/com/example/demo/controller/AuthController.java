package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.model.UserWallet;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserWalletRepository;
import com.example.demo.services.AuthService;
import com.example.demo.services.RpcWalletService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling user authentication and registration.
 *
 * <p>TODO: Implement JWT token generation and validation.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

  AuthService authService;
  RpcWalletService walletService;
  UserRepository userRepository;
  UserProfileRepository userProfileRepository;
  UserWalletRepository userWalletRepository;

  /**
   * Constructs the AuthController with required dependencies.
   *
   * @param authService           service for authentication logic
   * @param walletService         service for creating and managing RPC wallets
   * @param userRepository        repository for User entity operations
   * @param userProfileRepository repository for UserProfile entity operations
   * @param userWalletRepository  repository for UserWallet entity operations
   */
  public AuthController(AuthService authService, RpcWalletService walletService,
                        UserRepository userRepository, UserProfileRepository userProfileRepository,
                        UserWalletRepository userWalletRepository) {
    this.authService = authService;
    this.walletService = walletService;
    this.userRepository = userRepository;
    this.userProfileRepository = userProfileRepository;
    this.userWalletRepository = userWalletRepository;
  }

  /**
   * Registers a new user and provisions a new wallet.
   *
   * <p><b>Warning:</b> Passwords are currently stored in plain text.
   *
   * @param body map containing displayName, email, and password
   * @return response containing the user's email and display name, or an error status
   */
  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
    String displayName = body.get("displayName");
    String email = body.get("email");
    String password = body.get("password");

    if (userRepository.findByEmail(email).isPresent()) {
      return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
    }

    if (userProfileRepository.findByDisplayName(displayName).isPresent()) {
      return ResponseEntity.badRequest().body(Map.of("message", "Display name already in use"));
    }

    User user = authService.register(displayName, email, password);
    final RpcWalletService.WalletCredentials walletCredentials;
    try {
      walletCredentials = walletService.createWallet();
    } catch (IllegalStateException e) {
      LOGGER.error("Signup failed while creating wallet for {}", email, e);
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              Map.of(
                  "error", "Could not create wallet for new user",
                  "details", e.getMessage()));
    }


    UserWallet wallet = new UserWallet();
    wallet.setUser(user);
    wallet.setWalletAddress(walletCredentials.walletId());
    wallet.setWalletPrivateKey(walletCredentials.walletPrivateKey());
    userWalletRepository.save(wallet);
    return ResponseEntity.ok(
        Map.of("email", user.getEmail(), "displayName", user.getUserProfile().getDisplayName()));
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
   * @return the user's profile information (currently a placeholder)
   */
  @GetMapping("/profile")
  public ResponseEntity<?> getProfile(@CookieValue(name = "jwt") String token) {
    return ResponseEntity.ok(Map.of("message",
        "This endpoint will return the user's profile information based on the JWT token in a future implementation."));
  }

  /**
   * Logs out the user by clearing their authentication cookie.
   *
   * @param response the HTTP response used to clear the JWT cookie
   * @return a success message
   */
  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletResponse response) {
    Cookie cookie = new Cookie("jwt", null);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);

    return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
  }
}
