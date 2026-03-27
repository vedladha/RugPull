package edu.wisc.t32.controller;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.wisc.t32.config.CorsConfig;
import edu.wisc.t32.config.SecurityConfig;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.UserProfileRepository;
import edu.wisc.t32.repository.UserRepository;
import edu.wisc.t32.repository.UserWalletRepository;
import edu.wisc.t32.services.AuthService;
import edu.wisc.t32.services.RpcWalletService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Endpoint tests for {@link UserController}.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private UserProfileRepository userProfileRepository;

  @MockitoBean
  private UserWalletRepository userWalletRepository;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private RpcWalletService rpcWalletService;

  // Checks that /api/users returns the current users list.
  @Test
  void getAllUsers_returnsUsers_whenUsersExist() throws Exception {
    when(userRepository.findAll()).thenReturn(List.of(
        buildUser(1, "first@example.com"),
        buildUser(2, "second@example.com")));

    mockMvc.perform(get("/api/users").header(ORIGIN, "http://localhost:3000"))
        .andExpect(status().isOk())
        .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].userId").value(1))
        .andExpect(jsonPath("$[0].email").value("first@example.com"))
        .andExpect(jsonPath("$[1].userId").value(2))
        .andExpect(jsonPath("$[1].email").value("second@example.com"));
  }

  // Checks that /api/users returns an empty list when there are no users.
  @Test
  void getAllUsers_returnsEmptyArray_whenNoUsersExist() throws Exception {
    when(userRepository.findAll()).thenReturn(List.of());

    mockMvc.perform(get("/api/users").header(ORIGIN, "http://localhost:3000"))
        .andExpect(status().isOk())
        .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }

  private User buildUser(int userId, String email) {
    User user = new User();
    user.setUserId(userId);
    user.setEmail(email);
    user.setDeleted(false);
    return user;
  }
}
