package edu.wisc.t32.services;

import edu.wisc.t32.enums.UserStatus;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service class for handling user authentication and registration logic.
 *
 * <p>Provides methods for registering new users and authenticating existing ones,
 * utilizing BCrypt for secure password hashing.
 */
@Service
public class AuthService {

  UserRepository userRepo;

  /**
   * Constructs an {@code AuthService} with the specified user repository.
   *
   * @param userRepo the repository used for user data operations
   */
  public AuthService(UserRepository userRepo) {
    this.userRepo = userRepo;
  }

  /**
   * Registers a new user account and creates an associated user profile.
   *
   * <p>The provided raw password is securely hashed using BCrypt before the user
   * is saved to the database.
   *
   * @param displayName the display name for the user's profile
   * @param email       the email address for the user's account, which must be unique
   * @param password    the raw password for the user's account
   * @return the created {@link User} object, complete with its associated {@link UserProfile}
   */
  public User register(String displayName, String email, String password) {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String hashedPassword = encoder.encode(password);

    User user = new User();
    user.setEmail(email);
    user.setStatus(UserStatus.ACTIVE);
    user.setPasswordHash(hashedPassword);
    user.setDeleted(false);

    UserProfile profile = new UserProfile();
    profile.setDisplayName(displayName);
    profile.setUser(user);

    user.setUserProfile(profile);

    return userRepo.save(user);
  }

  /**
   * Authenticates a user based on their email and password.
   *
   * <p>Retrieves the user by email and verifies the provided raw password against
   * the stored BCrypt hash.
   *
   * @param email    the email address provided by the user for authentication
   * @param password the raw password provided by the user for authentication
   * @return the authenticated {@link User} object if the credentials are valid
   * @throws RuntimeException if the user is not found or the password does not match
   */
  public User login(String email, String password) {
    return userRepo.findByEmailAndDeletedFalse(email)
        .filter(user -> {
          BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
          return encoder.matches(password, user.getPasswordHash());
        })
        .orElseThrow(() -> new RuntimeException("Invalid email or password"));
  }
}
