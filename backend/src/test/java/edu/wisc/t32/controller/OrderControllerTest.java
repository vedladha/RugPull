package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.OrderCreateRequest;
import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.OrderRepository;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {
  private static final String VALID_TOKEN = "valid-token";

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private OrderService orderService;

  @Mock
  private CurrentUserService currentUserService;

  @InjectMocks
  private OrderController orderController;

  // Checks that a valid order request creates the order and reduces stock.
  @Test
  void createOrder_returnsCreatedOrder_whenRequestIsValid() {
    OrderCreateRequest request = buildRequest(4, 2);
    User currentUser = buildUser(7);
    Order savedOrder = buildOrder(1, 7, 4, 2, "pending");
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(currentUser, request)).thenReturn(savedOrder);

    ResponseEntity<?> response = orderController.createOrder(VALID_TOKEN, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    Order saved = (Order) body.get("order");
    assertNotNull(saved);
    assertEquals(1, saved.getOrderId());
    assertEquals(7, saved.getUserId());
    assertEquals(4, saved.getItemId());
    assertEquals(2, saved.getQuantity());
    assertEquals(0, new BigDecimal("12.50").compareTo(saved.getPrice()));
    assertEquals(0, new BigDecimal("2.50").compareTo(saved.getFeePercentage()));
    assertEquals("pending", saved.getOrderStatus());
  }

  // Checks that creating an order without auth returns 401.
  @Test
  void createOrder_returnsUnauthorized_whenUserIsNotAuthenticated() {
    OrderCreateRequest request = buildRequest(4, 2);
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = orderController.createOrder(null, request);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  // Checks that a missing request body returns 400.
  @Test
  void createOrder_returnsBadRequest_whenRequestBodyMissing() {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));

    ResponseEntity<?> response = orderController.createOrder(VALID_TOKEN, null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Request body is required", body.get("error"));
    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  // Checks that a missing item id returns 400.
  @Test
  void createOrder_returnsBadRequest_whenItemIdIsMissing() {
    OrderCreateRequest request = buildRequest(null, 2);
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));

    ResponseEntity<?> response = orderController.createOrder(VALID_TOKEN, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("itemId is required", body.get("error"));
    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  // Checks that a missing quantity returns 400.
  @Test
  void createOrder_returnsBadRequest_whenQuantityIsMissing() {
    OrderCreateRequest request = buildRequest(4, null);
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));

    ResponseEntity<?> response = orderController.createOrder(VALID_TOKEN, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("quantity is required", body.get("error"));
    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  // Checks that a non-positive quantity returns 400.
  @Test
  void createOrder_returnsBadRequest_whenQuantityIsNotPositive() {
    OrderCreateRequest request = buildRequest(4, 0);
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));

    ResponseEntity<?> response = orderController.createOrder(VALID_TOKEN, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("quantity must be greater than 0", body.get("error"));
    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  // Checks that ordering a missing item returns 404.
  @Test
  void createOrder_returnsNotFound_whenItemDoesNotExist() {
    OrderCreateRequest request = buildRequest(99, 2);
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(currentUser, request))
        .thenThrow(new NoSuchElementException("Item not found"));

    ResponseEntity<?> response = orderController.createOrder(VALID_TOKEN, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item not found", body.get("error"));
  }

  // Checks that ordering more than the available stock returns 400.
  @Test
  void createOrder_returnsBadRequest_whenStockIsInsufficient() {
    OrderCreateRequest request = buildRequest(4, 6);
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(currentUser, request))
        .thenThrow(new IllegalStateException("insufficient stock"));

    ResponseEntity<?> response = orderController.createOrder(VALID_TOKEN, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("insufficient stock", body.get("error"));
  }

  // Checks that listing orders returns the authenticated user's orders.
  @Test
  void getOrders_returnsOrdersForAuthenticatedUser() {
    Order order1 = buildOrder(1, 7, 4, 2, "pending");
    Order order2 = buildOrder(2, 7, 6, 1, "completed");
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(buildUser(7)));
    when(orderRepository.findByUserIdOrderByCreatedAtDesc(7)).thenReturn(List.of(order1, order2));

    ResponseEntity<?> response = orderController.getOrders(VALID_TOKEN);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    List<?> orders = (List<?>) body.get("orders");
    assertNotNull(orders);
    assertEquals(2, orders.size());
  }

  // Checks that listing orders returns an empty list when the user has none.
  @Test
  void getOrders_returnsEmptyList_whenUserHasNoOrders() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(buildUser(7)));
    when(orderRepository.findByUserIdOrderByCreatedAtDesc(7)).thenReturn(List.of());

    ResponseEntity<?> response = orderController.getOrders(VALID_TOKEN);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    List<?> orders = (List<?>) body.get("orders");
    assertNotNull(orders);
    assertEquals(0, orders.size());
  }

  // Checks that listing orders without auth returns 401.
  @Test
  void getOrders_returnsUnauthorized_whenUserIsNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = orderController.getOrders(null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
  }

  // Checks that getting one order returns the order when it belongs to the user.
  @Test
  void getOrder_returnsOrder_whenOrderExistsForUser() {
    Order order = buildOrder(5, 7, 4, 2, "pending");
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(buildUser(7)));
    when(orderRepository.findByOrderIdAndUserId(5, 7)).thenReturn(Optional.of(order));

    ResponseEntity<?> response = orderController.getOrder(VALID_TOKEN, 5);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    Order returnedOrder = (Order) body.get("order");
    assertNotNull(returnedOrder);
    assertEquals(5, returnedOrder.getOrderId());
    assertEquals(7, returnedOrder.getUserId());
  }

  // Checks that getting a missing order returns 404.
  @Test
  void getOrder_returnsNotFound_whenOrderDoesNotExist() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(buildUser(7)));
    when(orderRepository.findByOrderIdAndUserId(9, 7)).thenReturn(Optional.empty());

    ResponseEntity<?> response = orderController.getOrder(VALID_TOKEN, 9);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Order not found", body.get("error"));
  }

  // Checks that getting another user's order returns 404.
  @Test
  void getOrder_returnsNotFound_whenOrderBelongsToAnotherUser() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(buildUser(7)));
    when(orderRepository.findByOrderIdAndUserId(11, 7)).thenReturn(Optional.empty());

    ResponseEntity<?> response = orderController.getOrder(VALID_TOKEN, 11);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Order not found", body.get("error"));
  }

  // Checks that getting one order without auth returns 401.
  @Test
  void getOrder_returnsUnauthorized_whenUserIsNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = orderController.getOrder(null, 5);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
  }

  private OrderCreateRequest buildRequest(Integer itemId, Integer quantity) {
    OrderCreateRequest request = new OrderCreateRequest();
    request.setItemId(itemId);
    request.setQuantity(quantity);
    return request;
  }

  private Order buildOrder(
      Integer orderId,
      Integer userId,
      Integer itemId,
      Integer quantity,
      String orderStatus) {
    Order order = new Order();
    order.setOrderId(orderId);
    order.setUserId(userId);
    order.setItemId(itemId);
    order.setQuantity(quantity);
    order.setPrice(new BigDecimal("12.50"));
    order.setFeePercentage(new BigDecimal("2.50"));
    order.setOrderStatus(orderStatus);
    return order;
  }

  private User buildUser(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    return user;
  }
}
