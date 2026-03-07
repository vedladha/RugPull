package edu.wisc.t32.controller;

import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.repository.UserProfileRepository;
import edu.wisc.t32.repository.UserRepository;
import edu.wisc.t32.services.AuthService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * User controller which manages the api endpoints.
 */
@RestController
@RequestMapping("/api")
public class UserController {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final AuthService authService;

  /**
   * Initializes the user controller.
   *
   * @param userRepository        the user repository
   * @param userProfileRepository the profile repository
   * @param authService           the auth service
   */
  public UserController(UserRepository userRepository, UserProfileRepository userProfileRepository,
                        AuthService authService) {
    this.userRepository = userRepository;
    this.userProfileRepository = userProfileRepository;
    this.authService = authService;
  }

  /**
   * Gets all users from the user repository.
   *
   * @return a list of users
   */
  @GetMapping("/users")
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  /**
   * Adds a user to the user controller.
   *
   * @param email        the email to add
   * @param passwordHash the hashed psasword
   * @return a response
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
   * Put mapping that allows updating a user profile.
   *
   * @param userId      the userId to update
   * @param displayName the new display name
   * @param bio         the bio to change
   * @return a response
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
   * Gets a profile from a given userId.
   *
   * @param userId the userId to find a profile from
   * @return a response
   */
  @GetMapping("/users/{userId}/profile")
  public ResponseEntity<?> getProfile(@PathVariable Integer userId) {
    return userProfileRepository.findById(userId)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
