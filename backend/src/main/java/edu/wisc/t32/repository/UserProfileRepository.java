package edu.wisc.t32.repository;

import edu.wisc.t32.model.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The JPA repository for user profiles used to interact with the database.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {
  /**
   * Finds a user profile from a given user profile.
   *
   * @param displayName the displayName to find a profile from
   * @return a possible user profile
   */
  Optional<UserProfile> findByDisplayName(String displayName);
}
