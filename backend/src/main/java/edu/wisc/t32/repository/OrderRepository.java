package edu.wisc.t32.repository;

import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for managing {@link Order} entities.
 * Provides query methods used to scope orders to the authenticated user.
 */
public interface OrderRepository extends JpaRepository<Order, Integer> {

  /**
   * Retrieves all orders for a given user, newest first.
   *
   * @param user the user object
   * @return the user's orders sorted by creation time in descending order
   */
  List<Order> findByUserOrderByCreatedAtDesc(User user);

  /**
   * Retrieves a single order only when it belongs to the given user.
   *
   * @param orderId the unique identifier of the order
   * @param user the user object
   * @return the matching order if it belongs to the given user
   */
  Optional<Order> findByOrderIdAndUser(Integer orderId, User user);

  /**
   * Retrieves all order for a given user where they are the seller
   * 
   * @param user the user object
   * @return the user's orders sorted by creation time in descending order
   */
  List<Order> findDistinctByItemsUserUserIdOrderByCreatedAtDesc(User user);
}
