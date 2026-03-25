package com.example.demo.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private JwtUtil jwtUtil;

  @InjectMocks
  private CurrentUserService currentUserService;

  // Checks that a valid token resolves to the matching user.
  @Test
  void getAuthenticatedUser_returnsUser_whenTokenIsValid() {
    User user = new User();
    user.setEmail("test@example.com");

    when(jwtUtil.extractEmail("valid-token")).thenReturn("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    Optional<User> result = currentUserService.getAuthenticatedUser("valid-token");

    assertTrue(result.isPresent());
    assertEquals("test@example.com", result.get().getEmail());
  }

  // Checks that a blank token is treated as unauthenticated.
  @Test
  void getAuthenticatedUser_returnsEmpty_whenTokenIsBlank() {
    Optional<User> result = currentUserService.getAuthenticatedUser(" ");

    assertFalse(result.isPresent());
  }

  // Checks that an invalid token is rejected.
  @Test
  void getAuthenticatedUser_returnsEmpty_whenTokenCannotBeParsed() {
    when(jwtUtil.extractEmail("bad-token")).thenThrow(new JwtException("Invalid token"));

    Optional<User> result = currentUserService.getAuthenticatedUser("bad-token");

    assertFalse(result.isPresent());
  }
}
