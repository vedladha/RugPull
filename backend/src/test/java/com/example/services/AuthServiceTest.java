package com.example.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.enums.UserStatus;
import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserRepository;
import com.example.demo.services.AuthService;
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
