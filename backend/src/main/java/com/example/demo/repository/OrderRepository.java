package com.example.demo.repository;

import com.example.demo.model.Order;
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
   * @param userId the unique identifier of the user
   * @return the user's orders sorted by creation time in descending order
   */
  List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);

  /**
   * Retrieves a single order only when it belongs to the given user.
   *
   * @param orderId the unique identifier of the order
   * @param userId the unique identifier of the user
   * @return the matching order if it belongs to the given user
   */
  Optional<Order> findByOrderIdAndUserId(Integer orderId, Integer userId);
}
