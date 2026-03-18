package com.example.demo.repository;

import com.example.demo.model.Wishlist;
import com.example.demo.model.WishlistId;
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
