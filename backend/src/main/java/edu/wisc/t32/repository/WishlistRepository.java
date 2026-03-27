package edu.wisc.t32.repository;

import edu.wisc.t32.model.Wishlist;
import edu.wisc.t32.model.WishlistId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for managing {@link Wishlist} entities.
 */
public interface WishlistRepository extends JpaRepository<Wishlist, WishlistId> {

  /**
   * Retrieves all wishlist entries for a given user.
   *
   * @param userId the unique identifier of the user
   * @return a list of wishlist entries belonging to the user
   */
  List<Wishlist> findByUserId(Integer userId);
}
