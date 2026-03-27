package edu.wisc.t32.controller;

import edu.wisc.t32.dto.ProfileUpdateRequest;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.repository.UserProfileRepository;
import edu.wisc.t32.services.CurrentUserService;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing and retrieving user profiles.
 * Provides endpoints for fetching public profiles and allowing authenticated users
 * to view and update their own profile information.
 */
@RestController
@RequestMapping("/profile")
public class UserProfileController {

  private final CurrentUserService currentUserService;
  private final UserProfileRepository userProfileRepository;

  /**
   * Constructs a new {@code UserProfileController}.
   *
   * @param currentUserService    the service used to retrieve the currently authenticated user
   * @param userProfileRepository the repository for accessing user profile data
   */
  public UserProfileController(CurrentUserService currentUserService,
                               UserProfileRepository userProfileRepository) {
    this.currentUserService = currentUserService;
    this.userProfileRepository = userProfileRepository;
  }

  /**
   * Retrieves a specific user's profile by their unique ID.
   *
   * @param userId the unique identifier of the user whose profile is being requested
   * @return a {@link ResponseEntity} containing a map with the "profile" object if found,
   *        or a 404 NOT FOUND status with an error message if the profile does not exist
   */
  @GetMapping("/{userId}")
  public ResponseEntity<?> getUserProfile(@PathVariable Integer userId) {
    final Optional<UserProfile> profile = userProfileRepository.findById(userId);
    if (profile.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error",
          "Profile not found"));
    }

    return ResponseEntity.ok(Map.of("profile", profile.get()));
  }

  /**
   * Retrieves the profile of the currently authenticated user.
   *
   * @param token the JWT authentication token extracted from the request cookies
   * @return a {@link ResponseEntity} containing the authenticated user's "profile",
   *         or a 401 UNAUTHORIZED status if the token is invalid or missing
   */
  @GetMapping("/me")
  public ResponseEntity<?> getMyProfile(@CookieValue(name = "jwt", required = false) String token) {
    final Optional<User> user = currentUserService.getAuthenticatedUser(token);
    if (user.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error",
          "Authentication required"));
    }

    final UserProfile profile = user.get().getUserProfile();

    return ResponseEntity.ok(Map.of("profile", profile));
  }

  /**
   * Updates the profile of the currently authenticated user.
   * Supports both full (PUT) and partial (PATCH) updates.
   *
   * <p>If a new display name is provided, it is checked for uniqueness against the database.
   * If a bio is provided and is blank, it will be saved as an empty string.
   *
   * @param token   the JWT authentication token extracted from the request cookies
   * @param request a {@link ProfileUpdateRequest} containing the new display name and/or bio
   * @return a {@link ResponseEntity} containing the updated "profile",
   *        a 401 UNAUTHORIZED status if the token is invalid/missing,
   *        or a 409 CONFLICT if the requested display name is already in use by another user
   */
  @RequestMapping(value = "/me", method = {RequestMethod.PUT, RequestMethod.PATCH})
  public ResponseEntity<?> putPatchMyProfile(
      @CookieValue(name = "jwt", required = false) String token,
      @RequestBody ProfileUpdateRequest request) {

    final Optional<User> user = currentUserService.getAuthenticatedUser(token);
    if (user.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    final UserProfile profile = user.get().getUserProfile();

    String displayName = request.getDisplayName();
    if (displayName != null && !displayName.isBlank()) {
      // check if name is not taken
      Optional<UserProfile> matching =
          userProfileRepository.findByDisplayName(request.getDisplayName());

      // If the name exists and belongs to someone else, reject the update
      if (matching.isPresent() && !matching.get().getUserId().equals(profile.getUserId())) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "Displayname already taken"));
      }

      profile.setDisplayName(displayName);
    }

    String bio = request.getBio();
    if (bio != null) {
      if (bio.isBlank()) {
        bio = "";
      }
      profile.setBio(bio);
    }

    userProfileRepository.save(profile);
    return ResponseEntity.ok(Map.of("profile", profile));
  }
}
