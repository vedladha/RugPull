package edu.wisc.t32.repository;

import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for {@link OrderItem} entities.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
  /**
   * Finds all items associated with a specific order by its primary identifier.
   *
   * @param orderId the unique ID of the parent order
   * @return a list of line items belonging to the specified order
   */
  List<OrderItem> findByOrderOrderId(Integer orderId);

  /**
   * Finds all order records containing a specific product.
   *
   * @param item the item entity to search for
   * @return a list of order entries containing the given item
   */
  List<OrderItem> findByItem(Item item);

  /**
   * Checks whether the given user has an order containing the specified item.
   * Used to verify that a user has purchased an item before allowing them
   * to leave a rating on it.
   *
   * @param userId the ID of the user
   * @param itemId the ID of the item
   * @return true if the user has at least one order containing the item
   */
  @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi "
      + "WHERE oi.order.user.userId = :userId AND oi.item.itemId = :itemId")
  boolean existsByUserIdAndItemId(@Param("userId") Integer userId,
                                  @Param("itemId") Integer itemId);
}
