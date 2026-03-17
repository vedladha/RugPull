package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserWalletRepository;
import com.example.demo.services.AuthService;
import com.example.demo.services.CurrentUserService;
import com.example.demo.services.RpcWalletService;
import com.example.demo.util.JwtUtil;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private AuthService authService;

  @Mock
  private RpcWalletService walletService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserProfileRepository userProfileRepository;

  @Mock
  private UserWalletRepository userWalletRepository;

  @Mock
  private JwtUtil jwtUtil;

  @Mock
  private CurrentUserService currentUserService;

  @InjectMocks
  private AuthController authController;

  // Checks that logging in sets the JWT cookie and returns 200.
  @Test
  void login_returnsOkAndSetsJwtCookie_whenCredentialsAreValid() {
    User user = buildUser("test@example.com", "TestUser");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(authService.login("test@example.com", "password")).thenReturn(user);
    when(jwtUtil.generateToken("test@example.com")).thenReturn("jwt-token");

    ResponseEntity<?> result = authController.login(
        Map.of("email", "test@example.com", "password", "password"), response);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
    assertNotNull(setCookie);
    assertTrue(setCookie.contains("jwt=jwt-token"));
    assertTrue(setCookie.contains("HttpOnly"));
    assertTrue(setCookie.contains("SameSite=Lax"));
  }

  // Checks that profile returns the authenticated user's basic info.
  @Test
  void getProfile_returnsAuthenticatedUser_whenJwtResolvesToUser() {
    User user = buildUser("test@example.com", "TestUser");
    when(currentUserService.getAuthenticatedUser("valid-token")).thenReturn(Optional.of(user));

    ResponseEntity<?> result = authController.getProfile("valid-token");

    assertEquals(HttpStatus.OK, result.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) result.getBody();
    assertNotNull(body);
    Map<?, ?> returnedUser = (Map<?, ?>) body.get("user");
    assertNotNull(returnedUser);
    assertEquals("test@example.com", returnedUser.get("email"));
    assertEquals("TestUser", returnedUser.get("displayName"));
  }

  // Checks that profile returns 401 when no valid JWT is present.
  @Test
  void getProfile_returnsUnauthorized_whenJwtIsMissingOrInvalid() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> result = authController.getProfile(null);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) result.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
  }

  private User buildUser(String email, String displayName) {
    User user = new User();
    user.setEmail(email);

    UserProfile userProfile = new UserProfile();
    userProfile.setDisplayName(displayName);
    user.setUserProfile(userProfile);
    return user;
  }
}
