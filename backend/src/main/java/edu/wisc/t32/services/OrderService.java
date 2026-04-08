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
    validateRequest(request);

    // Initialize the order
    Order order = new Order(currentUser, OrderStatus.PENDING);

    for (OrderCreateRequest.ItemRequest itemRequest : request.getItems()) {
      Item item = itemRepository.findByItemIdAndDeletedFalseForUpdate(itemRequest.getItemId())
          .orElseThrow(() -> new OrderItemNotFoundException("Item not found."));

      // Reserve the stock
      reserveStock(item, itemRequest.getQuantity());

      // Add the item to the order
      order.addItemToOrder(item, itemRequest.getQuantity());
    }

    // Ensure the order contains all the information
    order.finalizeOrder();
      
    // Save the order and all the OrderItems
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

  /**
   * Checks that the quantity is valid for the current stock of an item
   * and reduces the stock by that quantity.
   * 
   * @param item The item to check the stock of
   * @param quantity The quantity of the item wanted
   * @throws InsufficientStockException when there is less stock than the quantity requested
   * @throws IllegalArgumentException if the quantity is not positive
   */
  public void reserveStock(Item item, Integer quantity) {
    if (quantity == null || quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be positive.");
    }
    if (item.getStock() < quantity) {
      throw new InsufficientStockException("Insufficient stock");
    }
    item.setStock(item.getStock() - quantity);
  }

  /**
   * Validates the order request to ensure it contains at least one item
   * and that all item details are present and valid.
   *
   * @param request the order payload to validate
   * @throws IllegalArgumentException if the request is null, empty, or contains invalid data
   */
  private void validateRequest(OrderCreateRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Order request cannot be null.");
    }
      
    if (request.getItems() == null || request.getItems().isEmpty()) {
      throw new IllegalArgumentException("Order must contain at least one item.");
    }

    for (OrderCreateRequest.ItemRequest item : request.getItems()) {
      if (item.getItemId() == null) {
        throw new IllegalArgumentException("itemId is required for all items.");
      }
      if (item.getQuantity() == null) {
        throw new IllegalArgumentException("quantity is required for all items.");
      }
      if (item.getQuantity() <= 0) {
        throw new IllegalArgumentException("quantity must be greater than 0.");
      }
    }
  }
}
