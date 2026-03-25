package com.example.demo.repository;

import com.example.demo.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing {@link User} entities.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations and includes
 * custom query methods specific to users.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

  /**
   * Retrieves a user by their unique email address.
   *
   * @param email the exact email address to search for
   * @return an {@link Optional} containing the user if found, or empty otherwise
   */
  Optional<User> findByEmail(String email);

  /**
   * Retrieves a non-deleted user by their unique email address.
   *
   * @param email the exact email address to search for
   * @return an {@link Optional} containing the user if found, or empty otherwise
   */
  Optional<User> findByEmailAndDeletedFalse(String email);
}

