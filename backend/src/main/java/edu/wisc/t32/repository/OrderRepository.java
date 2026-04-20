package edu.wisc.t32.repository;

import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
   * @param user    the user object
   * @return the matching order if it belongs to the given user
   */
  Optional<Order> findByOrderIdAndUser(Integer orderId, User user);

  /**
   * Retrieves all orders for a given user where they are the seller.
   *
   * @param userId the id of the user we are searching for
   * @return the user's orders where they are the seller sorted by creation time in desc order
   */
  @Query("SELECT DISTINCT o FROM Order o "
      + "JOIN o.items oi "
      + "JOIN oi.item i "
      + "WHERE i.userId = :userId ")
  List<Order> findCompletedOrdersWhereSeller(@Param("userId") Integer userId);
}
