package edu.wisc.t32.controller;

import edu.wisc.t32.dto.OrderCreateRequest;
import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.OrderItemRepository;
import edu.wisc.t32.repository.OrderRepository;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.OrderService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
  private final OrderItemRepository orderItemRepository;
  private final OrderRepository orderRepository;
  private final OrderService orderService;
  private final CurrentUserService currentUserService;

  /**
   * Constructs an OrderController with the dependencies needed to place orders.
   *
   * @param orderItemRepository repository used for saving individual items in an order
   * @param orderRepository repository used for saving orders
   * @param orderService service used for locked purchase processing
   * @param currentUserService service used to resolve the authenticated user
   */
  public OrderController(
      OrderItemRepository orderItemRepository,
      OrderRepository orderRepository,
      OrderService orderService,
      CurrentUserService currentUserService) {
    this.orderItemRepository = orderItemRepository;
    this.orderRepository = orderRepository;
    this.orderService = orderService;
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

    Order saved = orderService.createOrder(currentUser.get(), request);
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

    List<Order> orders = orderService.getOrderHistory(currentUser.get());
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
        orderRepository.findByOrderIdAndUserId(orderId, currentUser.get());
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
      return "Request body is required.";
    }
    if (request.getItems() == null || request.getItems().isEmpty()) {
      return "Items are required.";
    }
    for (OrderCreateRequest.ItemRequest itemRequest : request.getItems()) {
      if (itemRequest.getItemId() == null) { return "itemId is required for all items."; }
      if (itemRequest.getQuantity() == null) { return "quantity is required for all items."; }
      if (itemRequest.getQuantity() <= 0) { return " quantity must be > 0 for all items."; }
    }

    return null;
  }
}
