package edu.wisc.t32.services;

import edu.wisc.t32.dto.OrderCreateRequest;
import edu.wisc.t32.exception.InsufficientStockException;
import edu.wisc.t32.exception.OrderItemNotFoundException;
import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.OrderRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating orders while protecting stock updates with an item-row lock.
 */
@Service
public class OrderService {
  private static final BigDecimal DEFAULT_FEE_PERCENTAGE = new BigDecimal("2.50");
  private static final String PENDING_STATUS = "pending";

  private final OrderRepository orderRepository;
  private final ItemRepository itemRepository;

  /**
   * Constructs the service with the repositories needed to place orders.
   *
   * @param orderRepository repository used for saving orders
   * @param itemRepository repository used for locked item lookups and stock updates
   */
  public OrderService(OrderRepository orderRepository, ItemRepository itemRepository) {
    this.orderRepository = orderRepository;
    this.itemRepository = itemRepository;
  }

  /**
   * Creates a new order for the supplied authenticated user.
   *
   * <p>The item row is locked for the duration of the transaction so stock can be checked and
   * decremented safely under concurrent purchase attempts.
   *
   * @param currentUser the authenticated user placing the order
   * @param request the validated order request payload
   * @return the saved order
   * @throws OrderItemNotFoundException when the item does not exist
   * @throws InsufficientStockException when there is not enough stock left to fulfill the request
   */
  @Transactional
  public Order createOrder(User currentUser, OrderCreateRequest request) {
    Item item = itemRepository.findByItemIdAndDeletedFalseForUpdate(request.getItemId())
        .orElseThrow(() -> new OrderItemNotFoundException("Item not found"));

    if (item.getStock() == null || item.getStock() < request.getQuantity()) {
      throw new InsufficientStockException("insufficient stock");
    }

    item.setStock(item.getStock() - request.getQuantity());
    itemRepository.save(item);

    Order order = new Order();
    order.setUserId(currentUser.getUserId());
    order.setItemId(item.getItemId());
    order.setQuantity(request.getQuantity());
    order.setPrice(item.getPrice());
    order.setFeePercentage(DEFAULT_FEE_PERCENTAGE);
    order.setOrderStatus(PENDING_STATUS);

    return orderRepository.save(order);
  }
}
