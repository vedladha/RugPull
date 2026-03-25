package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.wisc.t32.exception.InsufficientStockException;
import edu.wisc.t32.exception.OrderItemNotFoundException;
import edu.wisc.t32.dto.OrderCreateRequest;
import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.OrderRepository;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(orderController)
        .setControllerAdvice(new OrderExceptionHandler())
        .build();
  }

  // Checks that a valid order request creates the order and reduces stock.
  @Test
  void createOrder_returnsCreatedOrder_whenRequestIsValid() throws Exception {
    User currentUser = buildUser(7);
    Order savedOrder = buildOrder(1, 7, 4, 2, "pending");
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(any(User.class), argThat(request ->
        request.getItemId().equals(4) && request.getQuantity().equals(2)))).thenReturn(savedOrder);

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(4, 2)))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.order.orderId").value(1))
        .andExpect(jsonPath("$.order.userId").value(7))
        .andExpect(jsonPath("$.order.itemId").value(4))
        .andExpect(jsonPath("$.order.quantity").value(2))
        .andExpect(jsonPath("$.order.price").value(12.5))
        .andExpect(jsonPath("$.order.feePercentage").value(2.5))
        .andExpect(jsonPath("$.order.orderStatus").value("pending"));
  }

  // Checks that creating an order without auth returns 401.
  @Test
  void createOrder_returnsUnauthorized_whenUserIsNotAuthenticated() throws Exception {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    mockMvc.perform(post("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(4, 2)))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Authentication required"));
    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  // Checks that a missing request body returns 400.
  @Test
  void createOrder_returnsBadRequest_whenRequestBodyMissing() throws Exception {
    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  // Checks that a missing item id returns 400.
  @Test
  void createOrder_returnsBadRequest_whenItemIdIsMissing() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"quantity\":2}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("itemId is required"));
    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  // Checks that a missing quantity returns 400.
  @Test
  void createOrder_returnsBadRequest_whenQuantityIsMissing() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"itemId\":4}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("quantity is required"));
    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  // Checks that a non-positive quantity returns 400.
  @Test
  void createOrder_returnsBadRequest_whenQuantityIsNotPositive() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(4, 0)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("quantity must be greater than 0"));
    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  // Checks that ordering a missing item returns 404.
  @Test
  void createOrder_returnsNotFound_whenItemDoesNotExist() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(any(User.class), argThat(request ->
        request.getItemId().equals(99) && request.getQuantity().equals(2))))
        .thenThrow(new OrderItemNotFoundException("Item not found"));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(99, 2)))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Item not found"));
  }

  // Checks that ordering more than the available stock returns 400.
  @Test
  void createOrder_returnsBadRequest_whenStockIsInsufficient() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(any(User.class), argThat(request ->
        request.getItemId().equals(4) && request.getQuantity().equals(6))))
        .thenThrow(new InsufficientStockException("insufficient stock"));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(4, 6)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("insufficient stock"));
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

  private String orderRequestJson(Integer itemId, Integer quantity) {
    return "{\"itemId\":" + itemId + ",\"quantity\":" + quantity + "}";
  }
}
