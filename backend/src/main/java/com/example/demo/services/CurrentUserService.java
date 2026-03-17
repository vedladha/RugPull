package com.example.demo.services;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Resolves the authenticated user for requests that carry a JWT cookie.
 */
@Service
public class CurrentUserService {

  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;

  /**
   * Constructs the service with the dependencies required to resolve users from JWTs.
   *
   * @param userRepository repository used to load users by email
   * @param jwtUtil utility used to verify and parse JWT tokens
   */
  public CurrentUserService(UserRepository userRepository, JwtUtil jwtUtil) {
    this.userRepository = userRepository;
    this.jwtUtil = jwtUtil;
  }

  /**
   * Resolves the current authenticated user from the supplied JWT string.
   *
   * @param token the raw JWT value from the request cookie
   * @return the matching user when the token is present, valid, and maps to an existing user
   */
  public Optional<User> getAuthenticatedUser(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }

    try {
      String email = jwtUtil.extractEmail(token);
      return userRepository.findByEmail(email);
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
