package com.example.demo.controller;

import com.example.demo.dto.OrderCreateRequest;
import com.example.demo.model.Item;
import com.example.demo.model.Order;
import com.example.demo.model.User;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.services.CurrentUserService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for placing orders.
 * This version supports a single item per order row.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {
  private static final BigDecimal DEFAULT_FEE_PERCENTAGE = new BigDecimal("2.50");
  private static final String PENDING_STATUS = "pending";

  private final OrderRepository orderRepository;
  private final ItemRepository itemRepository;
  private final CurrentUserService currentUserService;

  /**
   * Constructs an OrderController with the dependencies needed to place orders.
   *
   * @param orderRepository repository used for saving orders
   * @param itemRepository repository used for item lookups and stock updates
   * @param currentUserService service used to resolve the authenticated user
   */
  public OrderController(
      OrderRepository orderRepository,
      ItemRepository itemRepository,
      CurrentUserService currentUserService) {
    this.orderRepository = orderRepository;
    this.itemRepository = itemRepository;
    this.currentUserService = currentUserService;
  }

  /**
   * Places a new order for the authenticated user.
   *
   * @param token the JWT token extracted from the HTTP-only cookie
   * @param request the item and quantity being ordered
   * @return the created order, or an error if auth or validation fails
   */
  @PostMapping
  @Transactional
  public ResponseEntity<?> createOrder(
      @CookieValue(name = "jwt", required = false) String token,
      @RequestBody OrderCreateRequest request) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    String validationError = validate(request);
    if (validationError != null) {
      return ResponseEntity.badRequest().body(Map.of("error", validationError));
    }

    Optional<Item> existingItem = itemRepository.findByItemIdAndDeletedFalse(request.getItemId());
    if (existingItem.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Item not found"));
    }

    Item item = existingItem.get();
    if (item.getStock() == null || item.getStock() < request.getQuantity()) {
      return ResponseEntity.badRequest().body(Map.of("error", "insufficient stock"));
    }

    item.setStock(item.getStock() - request.getQuantity());
    itemRepository.save(item);

    Order order = new Order();
    order.setUserId(currentUser.get().getUserId());
    order.setItemId(item.getItemId());
    order.setQuantity(request.getQuantity());
    order.setPrice(item.getPrice());
    order.setFeePercentage(DEFAULT_FEE_PERCENTAGE);
    order.setOrderStatus(PENDING_STATUS);

    Order saved = orderRepository.save(order);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("order", saved));
  }

  /**
   * Retrieves all orders for the authenticated user.
   *
   * @param token the JWT token extracted from the HTTP-only cookie
   * @return the authenticated user's orders, or an auth error
   */
  @GetMapping
  public ResponseEntity<?> getOrders(@CookieValue(name = "jwt", required = false) String token) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    List<Order> orders =
        orderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.get().getUserId());
    return ResponseEntity.ok(Map.of("orders", orders));
  }

  /**
   * Retrieves one order for the authenticated user.
   *
   * @param token the JWT token extracted from the HTTP-only cookie
   * @param orderId the unique identifier of the order to retrieve
   * @return the matching order, or an error if it is missing or not owned by the user
   */
  @GetMapping("/{orderId}")
  public ResponseEntity<?> getOrder(
      @CookieValue(name = "jwt", required = false) String token,
      @PathVariable Integer orderId) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    Optional<Order> existing =
        orderRepository.findByOrderIdAndUserId(orderId, currentUser.get().getUserId());
    if (existing.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Order not found"));
    }

    return ResponseEntity.ok(Map.of("order", existing.get()));
  }

  /**
   * Validates the fields of an incoming {@link OrderCreateRequest}.
   *
   * @param request the order request payload to validate
   * @return a validation error message, or {@code null} when the request is valid
   */
  private String validate(OrderCreateRequest request) {
    if (request == null) {
      return "Request body is required";
    }
    if (request.getItemId() == null) {
      return "itemId is required";
    }
    if (request.getQuantity() == null) {
      return "quantity is required";
    }
    if (request.getQuantity() <= 0) {
      return "quantity must be greater than 0";
    }
    return null;
  }
}
