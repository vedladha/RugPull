package edu.wisc.t32.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.repository.UserProfileRepository;
import edu.wisc.t32.repository.UserRepository;
import edu.wisc.t32.repository.UserWalletRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Unit tests for {@link AuthService}.
 */
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
  @Mock
  UserRepository userRepo;

  @Mock
  UserProfileRepository userProfileRepo;

  @Mock
  UserWalletRepository userWalletRepo;

  @Mock
  RpcWalletService walletService;

  @InjectMocks
  AuthService authService;

  @Test
  void confirmRegistration() {
    User user = new User();
    user.setEmail("test@example.com");
    UserProfile profile = new UserProfile();
    profile.setDisplayName("testuser");
    user.setUserProfile(profile);

    when(userRepo.save(any(User.class))).thenReturn(user);

    User registeredUser = authService.register("testuser", "test@example.com", "password");

    assertEquals("test@example.com", registeredUser.getEmail());
    assertEquals("testuser", registeredUser.getUserProfile().getDisplayName());
    verify(userRepo, times(1)).save(any(User.class));
  }

  @Test
  void registerWithWallet_savesUserAndWallet_whenRequestIsValid() {
    User user = new User();
    user.setEmail("test@example.com");
    UserProfile profile = new UserProfile();
    profile.setDisplayName("testuser");
    user.setUserProfile(profile);

    when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.empty());
    when(userProfileRepo.findByDisplayName("testuser")).thenReturn(Optional.empty());
    when(userRepo.save(any(User.class))).thenReturn(user);
    when(walletService.createWallet()).thenReturn(
        new RpcWalletService.WalletCredentials("wallet-1", "private-key"));

    User registeredUser = authService.registerWithWallet("testuser", "test@example.com",
        "password");

    assertEquals("test@example.com", registeredUser.getEmail());
    assertEquals("testuser", registeredUser.getUserProfile().getDisplayName());
    verify(userRepo, times(1)).save(any(User.class));
    verify(userWalletRepo, times(1)).save(any());
  }

  @Test
  void registerWithWallet_rejectsDuplicateEmail() {
    when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> authService.registerWithWallet("testuser", "test@example.com", "password"));

    assertEquals("Email already exists", error.getMessage());
    verify(userRepo, never()).save(any(User.class));
    verify(userWalletRepo, never()).save(any());
  }

  @Test
  void registerWithWallet_rejectsDuplicateDisplayName() {
    UserProfile profile = new UserProfile();
    profile.setDisplayName("testuser");

    when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.empty());
    when(userProfileRepo.findByDisplayName("testuser")).thenReturn(Optional.of(profile));

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> authService.registerWithWallet("testuser", "test@example.com", "password"));

    assertEquals("Display name already in use", error.getMessage());
    verify(userRepo, never()).save(any(User.class));
    verify(userWalletRepo, never()).save(any());
  }

  @Test
  void registerWithWallet_propagatesWalletFailure() {
    User user = new User();
    user.setEmail("test@example.com");
    UserProfile profile = new UserProfile();
    profile.setDisplayName("testuser");
    user.setUserProfile(profile);

    when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.empty());
    when(userProfileRepo.findByDisplayName("testuser")).thenReturn(Optional.empty());
    when(userRepo.save(any(User.class))).thenReturn(user);
    when(walletService.createWallet()).thenThrow(new IllegalStateException("wallet failed"));

    IllegalStateException error = assertThrows(IllegalStateException.class,
        () -> authService.registerWithWallet("testuser", "test@example.com", "password"));

    assertEquals("wallet failed", error.getMessage());
    verify(userWalletRepo, never()).save(any());
  }

  @Test
  void confirmLogin() {
    User user = new User();
    user.setEmail("test@example.com");
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String hashedPassword = encoder.encode("password");
    user.setPasswordHash(hashedPassword);

    when(userRepo.findByEmailAndDeletedFalse("test@example.com")).thenReturn(Optional.of(user));

    User result = authService.login("test@example.com", "password");

    assertEquals("test@example.com", result.getEmail());
  }

  @Test
  void invalidPassword() {
    User user = new User();
    user.setEmail("test@example.com");
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String hashedPassword = encoder.encode("goodPassword");
    user.setPasswordHash(hashedPassword);

    when(userRepo.findByEmailAndDeletedFalse("test@example.com")).thenReturn(Optional.of(user));

    assertThrows(RuntimeException.class, () -> {
      authService.login("test@example.com", "badPassword");
    });
  }

  @Test
  void invalidEmail() {
    when(userRepo.findByEmailAndDeletedFalse("fake@example.com")).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> {
      authService.login("fake@example.com", "password");
    });
  }
}
