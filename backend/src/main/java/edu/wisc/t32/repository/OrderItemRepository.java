package edu.wisc.t32.repository;

import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
