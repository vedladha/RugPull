package com.example.demo.repository;

import com.example.demo.model.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing {@link UserProfile} entities.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations and includes
 * custom query methods specific to user profiles.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {

  /**
   * Retrieves a user profile by its unique display name.
   *
   * @param displayName the exact display name to search for
   * @return an {@link Optional} containing the user profile if found, or empty otherwise
   */
  Optional<UserProfile> findByDisplayName(String displayName);

  /**
   * Retrieves a user profile by its unique user ID.
   * 
   * @param userId the exact id to search for
   * @return an UserProfile containing the user profile if found, or empty otherwise
   */
  UserProfile findByUserId(Integer userId);
}
