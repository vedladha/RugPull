package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.example.demo.config.CorsConfig;
import com.example.demo.config.SecurityConfig;
import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.services.AuthService;
import com.example.demo.services.CurrentUserService;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Endpoint tests for {@link UserProfileController}.
 */
@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class UserProfileControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CurrentUserService currentUserService;

  @MockitoBean
  private UserProfileRepository userProfileRepository;

  @MockitoBean
  private AuthService authService;

  // --- GET /profile/{userId} Tests ---

  @Test
  void getUserProfile_returnsProfile_whenExists() throws Exception {
    User dummyUser = buildUser(1, "TestUser", "My biography");
    when(userProfileRepository.findById(1)).thenReturn(Optional.of(dummyUser.getUserProfile()));

    mockMvc.perform(get("/profile/1").header(ORIGIN, "http://localhost:3000"))
        .andExpect(status().isOk())
        .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.profile.displayName").value("TestUser"))
        .andExpect(jsonPath("$.profile.bio").value("My biography"));
  }

  @Test
  void getUserProfile_returns404_whenNotFound() throws Exception {
    when(userProfileRepository.findById(99)).thenReturn(Optional.empty());

    mockMvc.perform(get("/profile/99").header(ORIGIN, "http://localhost:3000"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Profile not found"));
  }

  // --- GET /profile/me Tests ---

  @Test
  void getMyProfile_returnsProfile_whenAuthenticated() throws Exception {
    User dummyUser = buildUser(1, "MyName", "My Bio");
    when(currentUserService.getAuthenticatedUser("valid-token")).thenReturn(Optional.of(dummyUser));

    mockMvc.perform(get("/profile/me")
            .header(ORIGIN, "http://localhost:3000")
            .cookie(new Cookie("jwt", "valid-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profile.displayName").value("MyName"));
  }

  @Test
  void getMyProfile_returns401_whenNotAuthenticated() throws Exception {
    when(currentUserService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

    mockMvc.perform(get("/profile/me").header(ORIGIN, "http://localhost:3000"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Authentication required"));
  }

  // --- PUT /profile/me Tests ---

  @Test
  void patchMyProfile_updatesProfile_whenValidDataProvided() throws Exception {
    User currentUser = buildUser(1, "OldName", "Old Bio");
    when(currentUserService.getAuthenticatedUser("valid-token")).thenReturn(
        Optional.of(currentUser));

    when(userProfileRepository.findByDisplayName("NewName")).thenReturn(Optional.empty());

    // Using a raw JSON string instead of ObjectMapper
    String rawJsonBody = "{ \"displayName\": \"NewName\", \"bio\": \"New Bio\" }";

    mockMvc.perform(put("/profile/me")
            .with(csrf())
            .header(ORIGIN, "http://localhost:3000")
            .cookie(new Cookie("jwt", "valid-token"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawJsonBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profile.displayName").value("NewName"))
        .andExpect(jsonPath("$.profile.bio").value("New Bio"));

    verify(userProfileRepository).save(currentUser.getUserProfile());
  }

  @Test
  void patchMyProfile_returns409_whenDisplayNameTakenByOtherUser() throws Exception {
    User currentUser = buildUser(1, "MyName", "My Bio");
    User otherUser = buildUser(2, "TakenName", "Their Bio");

    when(currentUserService.getAuthenticatedUser("valid-token")).thenReturn(
        Optional.of(currentUser));
    when(userProfileRepository.findByDisplayName("TakenName")).thenReturn(
        Optional.of(otherUser.getUserProfile()));

    // Raw JSON string
    String rawJsonBody = "{ \"displayName\": \"TakenName\" }";

    mockMvc.perform(put("/profile/me")
            .with(csrf())
            .header(ORIGIN, "http://localhost:3000")
            .cookie(new Cookie("jwt", "valid-token"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawJsonBody))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("Displayname already taken"));
  }

  @Test
  void patchMyProfile_allowsUpdate_whenDisplayNameIsSameAsCurrentUser() throws Exception {
    User currentUser = buildUser(1, "MyName", "My Bio");

    when(currentUserService.getAuthenticatedUser("valid-token")).thenReturn(
        Optional.of(currentUser));
    when(userProfileRepository.findByDisplayName("MyName")).thenReturn(
        Optional.of(currentUser.getUserProfile()));

    // Raw JSON string with escaped quotes
    String rawJsonBody = "{ \"displayName\": \"MyName\", \"bio\": \"Updated Bio\" }";

    mockMvc.perform(put("/profile/me")
        .with(csrf())
        .header(ORIGIN, "http://localhost:3000")
        .cookie(new Cookie("jwt", "valid-token"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(rawJsonBody));
  }

  // --- Helper Methods ---

  private User buildUser(int userId, String displayName, String bio) {
    User user = new User();
    user.setUserId(userId);
    user.setEmail("user" + userId + "@example.com");
    user.setDeleted(false);

    UserProfile profile = new UserProfile();
    profile.setUserId(userId);
    profile.setDisplayName(displayName);
    profile.setBio(bio);
    profile.setUser(user);

    user.setUserProfile(profile);

    return user;
  }
}
