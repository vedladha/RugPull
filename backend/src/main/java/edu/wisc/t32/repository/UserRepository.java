package edu.wisc.t32.repository;

import edu.wisc.t32.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A JPA repository used to interact with the users table.
 */
public interface UserRepository extends JpaRepository<User, Integer> {
  /**
   * Attempts to find a user by a given email address.
   *
   * @param email an email address
   * @return a possible user
   */
  Optional<User> findByEmail(String email);
}
