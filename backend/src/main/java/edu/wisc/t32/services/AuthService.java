package edu.wisc.t32.services;

import edu.wisc.t32.dto.UserRegisteredEvent;
import edu.wisc.t32.enums.UserStatus;
import edu.wisc.t32.exception.DuplicateDisplayNameException;
import edu.wisc.t32.exception.DuplicateEmailException;
import edu.wisc.t32.exception.InvalidCurrentPasswordException;
import edu.wisc.t32.exception.InvalidNewPasswordException;
import edu.wisc.t32.exception.WalletProvisioningException;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.UserProfileRepository;
import edu.wisc.t32.repository.UserRepository;
import edu.wisc.t32.repository.UserWalletRepository;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for handling user authentication and registration logic.
 *
 * <p>Provides methods for registering new users and authenticating existing ones,
 * utilizing BCrypt for secure password hashing.
 */
@Service
public class AuthService {

  private final UserRepository userRepo;
  private final UserProfileRepository userProfileRepo;
  private final UserWalletRepository userWalletRepo;
  private final RpcWalletService walletService;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  /**
   * Constructs an {@code AuthService} with the specified user repository.
   *
   * @param userRepo the repository used for user data operations
   * @param userProfileRepo the repository used for user profile lookups
   * @param userWalletRepo the repository used for wallet persistence
   * @param walletService the service used to provision wallets
   */
  public AuthService(UserRepository userRepo,
                     UserProfileRepository userProfileRepo,
                     UserWalletRepository userWalletRepo,
                     RpcWalletService walletService) {
    this.userRepo = userRepo;
    this.userProfileRepo = userProfileRepo;
    this.userWalletRepo = userWalletRepo;
    this.walletService = walletService;
  }

  /**
   * Registers a new user and provisions a wallet as one transactional workflow.
   *
   * <p>If wallet creation fails, the surrounding transaction is rolled back so the user and
   * profile are not left partially persisted.
   *
   * @param displayName the display name for the user's profile
   * @param email       the email address for the user's account
   * @param password    the raw password for the user's account
   * @return the created {@link User} object
   * @throws DuplicateEmailException if the email is already taken
   * @throws DuplicateDisplayNameException if the display name is already taken
   * @throws WalletProvisioningException if wallet provisioning fails
   */
  @Transactional
  public UserRegisteredEvent registerWithWallet(String displayName, String email, String password) {
    if (userRepo.findByEmail(email).isPresent()) {
      throw new DuplicateEmailException("Email already exists");
    }

    if (userProfileRepo.findByDisplayName(displayName).isPresent()) {
      throw new DuplicateDisplayNameException("Display name already in use");
    }

    User user = register(displayName, email, password);
    final RpcWalletService.WalletCredentials walletCredentials;
    try {
      walletCredentials = walletService.createWallet();
    } catch (RuntimeException exception) {
      throw new WalletProvisioningException(exception.getMessage(), exception);
    }

    UserWallet wallet = new UserWallet();
    wallet.setUserId(user.getUserId());
    wallet.setWalletAddress(walletCredentials.walletId());
    wallet.setWalletPrivateKey(walletCredentials.walletPrivateKey());
    userWalletRepo.save(wallet);

    return new UserRegisteredEvent(
      user.getUserId(),
      user.getEmail(),
      user.getStatus(),
      user.getUserProfile(),
      LocalDateTime.now()
    );
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
    String hashedPassword = passwordEncoder.encode(password);

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
   * Changes the password for an authenticated user.
   *
   * <p>The current password must match the stored hash. The new password must be non-blank and
   * different from the current password.
   *
   * @param user the authenticated user whose password is being changed
   * @param currentPassword the user's current raw password
   * @param newPassword the user's new raw password
   * @throws InvalidCurrentPasswordException if the current password is wrong
   * @throws InvalidNewPasswordException if the new password is blank or matches the current one
   */
  public void changePassword(User user, String currentPassword, String newPassword) {
    if (currentPassword == null || currentPassword.isBlank()
        || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new InvalidCurrentPasswordException("Current password is incorrect");
    }

    if (newPassword == null || newPassword.isBlank()) {
      throw new InvalidNewPasswordException("New password is required");
    }

    if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
      throw new InvalidNewPasswordException("New password must be different from current password");
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepo.save(user);
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
        .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
        .orElseThrow(() -> new RuntimeException("Invalid email or password"));
  }
}
