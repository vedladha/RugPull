package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserWalletRepository;
import com.example.demo.services.AuthService;
import com.example.demo.services.RpcWalletService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing user accounts and profiles.
 *
 * <p>Provides endpoints for retrieving users, adding new users, and managing user profiles.
 */
@RestController
@RequestMapping("/api")
public class UserController {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final UserWalletRepository userWalletRepository;
  private final AuthService authService;
  private final RpcWalletService rpcWalletService;

  /**
   * Constructs a new {@code UserController} with the specified repositories and services.
   *
   * @param userRepository        the repository for user data
   * @param userProfileRepository the repository for user profile data
   * @param userWalletRepository  the repository for user wallet data
   * @param authService           the service for authentication operations
   * @param rpcWalletService      the service for wallet operations
   */
  public UserController(UserRepository userRepository, UserProfileRepository userProfileRepository,
                        UserWalletRepository userWalletRepository,
                        AuthService authService, RpcWalletService rpcWalletService) {
    this.userRepository = userRepository;
    this.userWalletRepository = userWalletRepository;
    this.userProfileRepository = userProfileRepository;
    this.authService = authService;
    this.rpcWalletService = rpcWalletService;
  }

  /**
   * Retrieves a list of all users.
   *
   * @return a list containing all users in the system
   */
  @GetMapping("/users")
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  /**
   * Adds a new user to the system.
   *
   * <p>Checks if the provided email already exists before creating the user.
   *
   * @param email        the email address of the new user
   * @param passwordHash the hashed password for the new user
   * @return a {@link ResponseEntity} containing the created user, or a bad request response
   *        if the email already exists
   */
  @PostMapping("/users/addUser")
  public ResponseEntity<?> addNewUser(@RequestParam String email,
                                      @RequestParam String passwordHash
  ) {
    if (userRepository.findByEmail(email).isPresent()) {
      return ResponseEntity.badRequest().body("Email already exists");
    }

    User u = new User();
    u.setEmail(email);
    u.setPasswordHash(passwordHash);
    u.setDeleted(false);
    return ResponseEntity.ok(userRepository.save(u));
  }

  /**
   * Creates or updates the profile for a specific user.
   *
   * @param userId      the ID of the user whose profile is being updated
   * @param displayName the new display name for the user
   * @param bio         the new biographical information for the user
   * @return a {@link ResponseEntity} containing the updated profile, or a not found response
   *        if the user does not exist
   */
  @PutMapping("/users/{userId}/profile")
  public ResponseEntity<?> upsertProfile(@PathVariable Integer userId,
                                         @RequestParam(required = false) String displayName,
                                         @RequestParam(required = false) String bio) {

    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      return ResponseEntity.notFound().build();
    }

    UserProfile profile = userProfileRepository.findById(userId).orElse(new UserProfile());
    profile.setUser(user);
    profile.setDisplayName(displayName);
    profile.setBio(bio);

    return ResponseEntity.ok(userProfileRepository.save(profile));
  }

  /**
   * Retrieves the profile for a specific user.
   *
   * @param userId the ID of the user whose profile to retrieve
   * @return a {@link ResponseEntity} containing the user profile, or a not found response
   *        if the profile does not exist
   */
  @GetMapping("/users/{userId}/profile")
  public ResponseEntity<?> getProfile(@PathVariable Integer userId) {
    return userProfileRepository.findById(userId)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
