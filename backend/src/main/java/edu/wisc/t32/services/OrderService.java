package edu.wisc.t32.services;

import edu.wisc.t32.dto.OrderCreateRequest;
import edu.wisc.t32.enums.OrderStatus;
import edu.wisc.t32.exception.InsufficientStockException;
import edu.wisc.t32.exception.OrderItemNotFoundException;
import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.OrderItem;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.OrderItemRepository;
import edu.wisc.t32.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating orders while protecting stock updates with an item-row lock.
 */
@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final ItemRepository itemRepository;

  /**
   * Constructs the service with the repositories needed to place orders.
   *
   * @param orderRepository repository used for saving orders
   * @param orderItemRepository repository used for saving items within an order
   * @param itemRepository repository used for locked item lookups and stock updates
   */
  public OrderService(OrderRepository orderRepository,
                      OrderItemRepository orderItemRepository, 
                      ItemRepository itemRepository) {
    this.orderRepository = orderRepository;
    this.itemRepository = itemRepository;
    this.orderItemRepository = orderItemRepository;
  }

  /**
   * Creates a new order for the supplied authenticated user.
   *
   * The item row is locked for the duration of the transaction so stock can be checked and
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
    if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
      throw new IllegalArgumentException("Orders must contain at least one item.");
    }

    Order order = new Order(currentUser, OrderStatus.PENDING);
    orderRepository.save(order);

    BigDecimal totalPrice = BigDecimal.ZERO;

    for (OrderCreateRequest.ItemRequest itemRequest : request.getItems()) {
      // Get the item being ordered and ensure it is valid
      Item itemBeingOrdered = 
        itemRepository.findByItemIdAndDeletedFalseForUpdate(itemRequest.getItemId())
        .orElseThrow(() -> new OrderItemNotFoundException("Item not found."));

      // Validate stock
      if (itemRequest.getQuantity() <= 0) {
        throw new IllegalArgumentException("Order quantity must be positive.");
      }
      if (itemBeingOrdered.getStock() == null || itemBeingOrdered.getStock() < itemRequest.getQuantity()) {
        throw new InsufficientStockException("Insufficient stock.");
      }

      // Update inventory
      itemBeingOrdered.setStock(itemBeingOrdered.getStock() - itemRequest.getQuantity());

      // Create the order row
      OrderItem orderItem = 
        new OrderItem(order, itemBeingOrdered, itemRequest.getQuantity(), itemBeingOrdered.getPrice());
      orderItemRepository.save(orderItem);

      // Accumulate total price
      BigDecimal lineTotal = itemBeingOrdered.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
      totalPrice = totalPrice.add(lineTotal);
    }

    order.setTotalPrice(totalPrice);
    return orderRepository.save(order);
  }

  /**
   * Gets all orders previously placed by a user.
   * 
   * @param currentUser the authenticated user to get the order history of
   * @return a list containing all orders placed by the user
   */
  public List<Order> getOrderHistory(User currentUser) {
    return orderRepository.findByUserOrderByCreatedAtDesc(currentUser);
  }
}
