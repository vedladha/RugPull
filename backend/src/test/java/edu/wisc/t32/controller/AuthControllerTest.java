package edu.wisc.t32.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.wisc.t32.dto.PasswordChangeRequest;
import edu.wisc.t32.dto.UserRegisteredEvent;
import edu.wisc.t32.enums.UserStatus;
import edu.wisc.t32.exception.DuplicateDisplayNameException;
import edu.wisc.t32.exception.DuplicateEmailException;
import edu.wisc.t32.exception.InvalidCurrentPasswordException;
import edu.wisc.t32.exception.InvalidNewPasswordException;
import edu.wisc.t32.exception.WalletProvisioningException;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.services.AuthService;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private AuthService authService;

  @Mock
  private JwtUtil jwtUtil;

  @Mock
  private CurrentUserService currentUserService;

  @InjectMocks
  private AuthController authController;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(authController)
        .setControllerAdvice(new AuthExceptionHandler())
        .build();
  }

  // Checks that register returns the new user's basic info.
  @Test
  void register_returnsUserRegisteredEvent_whenRequestIsValid() throws Exception {
    String email = "test@example.com";
    String displayName = "TestUser";

    UserProfile profile = new UserProfile();
    profile.setDisplayName(displayName);

    UserRegisteredEvent expectedEvent = new UserRegisteredEvent(
        1,
        email,
        UserStatus.ACTIVE,
        profile,
        LocalDateTime.now()
    );

    when(authService.registerWithWallet(displayName, email, "password"))
        .thenReturn(expectedEvent);

    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(registerRequestJson(displayName, email, "password")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.displayName").value(displayName));
  }

  // Checks that register rejects an email that already exists.
  @Test
  void register_returnsBadRequest_whenEmailExists() throws Exception {
    when(authService.registerWithWallet("TestUser", "test@example.com", "password"))
        .thenThrow(new DuplicateEmailException("Email already exists"));

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerRequestJson("TestUser", "test@example.com", "password")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Email already exists"));
  }

  // Checks that register rejects a display name that is already in use.
  @Test
  void register_returnsBadRequest_whenDisplayNameExists() throws Exception {
    when(authService.registerWithWallet("TestUser", "test@example.com", "password"))
        .thenThrow(new DuplicateDisplayNameException("Display name already in use"));

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerRequestJson("TestUser", "test@example.com", "password")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Display name already in use"));
  }

  // Checks that register returns 503 when wallet creation fails.
  @Test
  void register_returnsServiceUnavailable_whenWalletCreationFails() throws Exception {
    String email = "test@example.com";
    String displayName = "TestUser";

    when(authService.registerWithWallet(displayName, email, "password"))
        .thenThrow(new WalletProvisioningException("Could not create wallet for new user", 
            new IllegalStateException("wallet failed")));

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerRequestJson(displayName, email, "password")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("Could not create wallet for new user"))
        .andExpect(jsonPath("$.details").value("wallet failed"));
  }

  // Checks that login returns the user info and sets the JWT cookie.
  @Test
  void login_returnsUserAndCookie_whenCredentialsAreValid() throws Exception {
    User user = buildUser("test@example.com", "TestUser");

    when(authService.login("test@example.com", "password")).thenReturn(user);
    when(jwtUtil.generateToken("test@example.com")).thenReturn("jwt-token");

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequestJson("test@example.com", "password")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt=jwt-token")))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.displayName").value("TestUser"));
  }

  // Checks that login returns 401 when the credentials are wrong.
  @Test
  void login_returnsUnauthorized_whenCredentialsAreInvalid() throws Exception {
    when(authService.login("test@example.com", "bad-password"))
        .thenThrow(new RuntimeException("Invalid email or password"));

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequestJson("test@example.com", "bad-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Invalid email or password"));
  }

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

  @Test
  void changePassword_returnsOk_whenUserIsAuthenticated() throws Exception {
    User user = buildUser("test@example.com", "TestUser");
    when(currentUserService.getAuthenticatedUser("valid-token")).thenReturn(Optional.of(user));

    mockMvc.perform(put("/auth/password")
            .cookie(new Cookie("jwt", "valid-token"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(changePasswordRequestJson("currentPassword", "newPassword123")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password updated successfully"));

    verify(authService).changePassword(user, "currentPassword", "newPassword123");
  }

  @Test
  void changePassword_returnsUnauthorized_whenUserIsNotAuthenticated() throws Exception {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    mockMvc.perform(put("/auth/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(changePasswordRequestJson("currentPassword", "newPassword123")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Authentication required"));

    verify(authService, never()).changePassword(any(User.class), anyString(), anyString());
  }

  @Test
  void changePassword_returnsBadRequest_whenCurrentPasswordIsWrong() throws Exception {
    User user = buildUser("test@example.com", "TestUser");
    when(currentUserService.getAuthenticatedUser("valid-token")).thenReturn(Optional.of(user));
    doThrow(new InvalidCurrentPasswordException("Current password is incorrect"))
        .when(authService).changePassword(user, "wrongPassword", "newPassword123");

    mockMvc.perform(put("/auth/password")
            .cookie(new Cookie("jwt", "valid-token"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(changePasswordRequestJson("wrongPassword", "newPassword123")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Current password is incorrect"));
  }

  @Test
  void changePassword_returnsBadRequest_whenNewPasswordIsInvalid() throws Exception {
    User user = buildUser("test@example.com", "TestUser");
    when(currentUserService.getAuthenticatedUser("valid-token")).thenReturn(Optional.of(user));
    doThrow(new InvalidNewPasswordException("New password is required"))
        .when(authService).changePassword(user, "currentPassword", "");

    mockMvc.perform(put("/auth/password")
            .cookie(new Cookie("jwt", "valid-token"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(changePasswordRequestJson("currentPassword", "")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("New password is required"));
  }

  private User buildUser(String email, String displayName) {
    User user = new User();
    user.setEmail(email);

    UserProfile userProfile = new UserProfile();
    userProfile.setDisplayName(displayName);
    user.setUserProfile(userProfile);
    return user;
  }

  private String registerRequestJson(String displayName, String email, String password) {
    return "{\"displayName\":\"" + displayName
        + "\",\"email\":\"" + email
        + "\",\"password\":\"" + password + "\"}";
  }

  private String loginRequestJson(String email, String password) {
    return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
  }

  private String changePasswordRequestJson(String currentPassword, String newPassword) {
    PasswordChangeRequest request = new PasswordChangeRequest();
    request.setCurrentPassword(currentPassword);
    request.setNewPassword(newPassword);

    return "{\"currentPassword\":\"" + request.getCurrentPassword()
        + "\",\"newPassword\":\"" + request.getNewPassword() + "\"}";
  }
}
