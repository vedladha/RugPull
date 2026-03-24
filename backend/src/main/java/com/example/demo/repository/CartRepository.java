package com.example.demo.repository;

import com.example.demo.model.Cart;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for managing {@link Cart} entities.
 */
public interface CartRepository extends JpaRepository<Cart, Integer> {

  /**
   * Retrieves all cart entries for a given user.
   *
   * @param userId the unique identifier of the user
   * @return a list of cart entries belonging to the user
   */
  List<Cart> findByUserId(Integer userId);

  /**
   * Finds a cart entry by user ID and item ID.
   *
   * @param userId the unique identifier of the user
   * @param itemId the unique identifier of the item
   * @return the cart entry if it exists
   */
  Optional<Cart> findByUserIdAndItemId(Integer userId, Integer itemId);
}
