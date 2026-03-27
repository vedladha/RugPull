package edu.wisc.t32.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.repository.UserProfileRepository;
import edu.wisc.t32.repository.UserRepository;
import edu.wisc.t32.repository.UserWalletRepository;
import edu.wisc.t32.services.AuthService;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.RpcWalletService;
import edu.wisc.t32.util.JwtUtil;
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

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
  }

  // Checks that register returns the new user's basic info.
  @Test
  void register_returnsUser_whenRequestIsValid() throws Exception {
    User user = buildUser("test@example.com", "TestUser");

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
    when(userProfileRepository.findByDisplayName("TestUser")).thenReturn(Optional.empty());
    when(authService.register("TestUser", "test@example.com", "password")).thenReturn(user);
    when(walletService.createWallet()).thenReturn(
        new RpcWalletService.WalletCredentials("wallet-1", "private-key"));

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerRequestJson("TestUser", "test@example.com", "password")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.displayName").value("TestUser"));
  }

  // Checks that register rejects an email that already exists.
  @Test
  void register_returnsBadRequest_whenEmailExists() throws Exception {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));

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
    UserProfile profile = new UserProfile();
    profile.setDisplayName("TestUser");

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
    when(userProfileRepository.findByDisplayName("TestUser")).thenReturn(Optional.of(profile));

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
    User user = buildUser("test@example.com", "TestUser");

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
    when(userProfileRepository.findByDisplayName("TestUser")).thenReturn(Optional.empty());
    when(authService.register("TestUser", "test@example.com", "password")).thenReturn(user);
    when(walletService.createWallet()).thenThrow(new IllegalStateException("wallet failed"));

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerRequestJson("TestUser", "test@example.com", "password")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Could not create wallet for new user"))
        .andExpect(jsonPath("$.details").value("wallet failed"));

    verify(userWalletRepository, never()).save(org.mockito.ArgumentMatchers.any());
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
}
