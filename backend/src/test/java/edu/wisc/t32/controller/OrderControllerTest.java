package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.wisc.t32.dto.OrderCreateRequest;
import edu.wisc.t32.enums.OrderStatus;
import edu.wisc.t32.exception.InsufficientBalanceException;
import edu.wisc.t32.exception.InsufficientStockException;
import edu.wisc.t32.exception.OrderItemNotFoundException;
import edu.wisc.t32.exception.OrderPaymentException;
import edu.wisc.t32.exception.SelfPurchaseException;
import edu.wisc.t32.model.Item;
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

  @Test
  void createOrderReturnsCreatedOrderWhenRequestIsValid() throws Exception {
    User currentUser = buildUser(7);
    Item laptop = buildItem(4, new BigDecimal("12.50"), 10);
    Order savedOrder = buildOrder(1, currentUser, List.of(laptop), List.of(2));

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(any(User.class), any(OrderCreateRequest.class)))
        .thenReturn(savedOrder);

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(4), List.of(2))))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.order.orderId").value(1))
        .andExpect(jsonPath("$.order.totalPrice").value(25.00))
        .andExpect(jsonPath("$.order.items[0].item.itemId").value(4))
        .andExpect(jsonPath("$.order.orderStatus").value("COMPLETED"));
  }

  @Test
  void createOrderReturnsUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    mockMvc.perform(post("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(4), List.of(2))))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Authentication required"));

    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  @Test
  void createOrderReturnsBadRequestWhenRequestBodyMissing() throws Exception {
    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  @Test
  void createOrderReturnsBadRequestWhenItemIdIsMissing() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"items\": [{\"quantity\": 2}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("An itemId is required for all items."));

    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  @Test
  void createOrderReturnsBadRequestWhenQuantityIsMissing() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"items\": [{\"itemId\": 4}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Quantity is required for all items."));

    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  @Test
  void createOrderReturnsBadRequestWhenQuantityIsNotPositive() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(4), List.of(0))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Quantity must be greater than 0 for all items."));

    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  @Test
  void createOrderReturnsNotFoundWhenItemDoesNotExist() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(
        any(User.class),
        argThat(request ->
            request.getItems().get(0).getItemId().equals(99)
                && request.getItems().get(0).getQuantity().equals(2)
        )))
        .thenThrow(new OrderItemNotFoundException("Item not found"));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(99), List.of(2))))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Item not found"));
  }

  @Test
  void createOrderReturnsBadRequestWhenStockIsInsufficient() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(
        any(User.class),
        argThat(request ->
            request.getItems().get(0).getItemId().equals(4)
                && request.getItems().get(0).getQuantity().equals(6)
        )))
        .thenThrow(new InsufficientStockException("insufficient stock"));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(4), List.of(6))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("insufficient stock"));
  }

  @Test
  void createOrderReturnsBadRequestWhenBuyerBalanceIsInsufficient() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(any(User.class), any(OrderCreateRequest.class)))
        .thenThrow(new InsufficientBalanceException("Insufficient RPC balance."));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(4), List.of(2))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Insufficient RPC balance."));
  }

  @Test
  void createOrderReturnsBadRequestWhenBuyerPurchasesOwnListing() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(any(User.class), any(OrderCreateRequest.class)))
        .thenThrow(new SelfPurchaseException("You cannot purchase your own listing."));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(4), List.of(1))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("You cannot purchase your own listing."));
  }

  @Test
  void createOrderReturnsInternalServerErrorWhenPaymentProcessingFails() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(any(User.class), any(OrderCreateRequest.class)))
        .thenThrow(new OrderPaymentException("Failed to pay one or more sellers."));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(4), List.of(1))))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Failed to pay one or more sellers."));
  }

  @Test
  void getOrdersReturnsOrdersForAuthenticatedUser() {
    User user = buildUser(7);
    Item itemA = buildItem(101, new BigDecimal("10.00"), 100);
    Item itemB = buildItem(102, new BigDecimal("20.00"), 100);
    Item itemC = buildItem(103, new BigDecimal("30.00"), 100);
    Order order1 = buildOrder(1, user, List.of(itemA, itemB), List.of(1, 1));
    Order order2 = buildOrder(2, user, List.of(itemC), List.of(2));

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(orderService.getOrderHistory(user)).thenReturn(List.of(order1, order2));

    ResponseEntity<?> response = orderController.getOrders(VALID_TOKEN);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);

    List<?> orders = (List<?>) body.get("orders");
    assertNotNull(orders);
    assertEquals(2, orders.size());

    Order firstReturned = (Order) orders.get(0);
    assertEquals(1, firstReturned.getOrderId());
    assertEquals(2, firstReturned.getItems().size());
    assertEquals(new BigDecimal("30.00"), firstReturned.getTotalPrice());
  }

  @Test
  void getOrdersReturnsEmptyListWhenUserHasNoOrders() {
    User user = buildUser(7);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));

    ResponseEntity<?> response = orderController.getOrders(VALID_TOKEN);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    List<?> orders = (List<?>) body.get("orders");
    assertNotNull(orders);
    assertEquals(0, orders.size());
  }

  @Test
  void getOrdersReturnsUnauthorizedWhenUserIsNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = orderController.getOrders(null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
  }

  @Test
  void getOrderReturnsOrderWhenOrderExistsForUser() {
    User user = buildUser(7);
    Item itemA = buildItem(101, new BigDecimal("10.00"), 100);
    Order order = buildOrder(5, user, List.of(itemA), List.of(2));

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(orderRepository.findByOrderIdAndUser(5, user))
        .thenReturn(Optional.of(order));

    ResponseEntity<?> response = orderController.getOrder(VALID_TOKEN, 5);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);

    Order returnedOrder = (Order) body.get("order");
    assertNotNull(returnedOrder);
    assertEquals(5, returnedOrder.getOrderId());
    assertEquals(7, returnedOrder.getUser().getUserId());
    assertEquals(new BigDecimal("20.00"), returnedOrder.getTotalPrice());
  }

  @Test
  void getOrderReturnsNotFoundWhenOrderDoesNotExist() {
    User user = buildUser(7);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(orderRepository.findByOrderIdAndUser(9, user))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = orderController.getOrder(VALID_TOKEN, 9);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Order not found", body.get("error"));
  }

  @Test
  void getOrderReturnsNotFoundWhenOrderBelongsToAnotherUser() {
    User user = buildUser(7);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(orderRepository.findByOrderIdAndUser(11, user))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = orderController.getOrder(VALID_TOKEN, 11);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Order not found", body.get("error"));
  }

  @Test
  void getOrderReturnsUnauthorizedWhenUserIsNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = orderController.getOrder(null, 5);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));

    verify(orderRepository, never()).findByOrderIdAndUser(anyInt(), any(User.class));
  }

  private Order buildOrder(
      Integer orderId,
      User user,
      List<Item> items,
      List<Integer> itemQuantities) {
    Order order = new Order(user);
    for (int i = 0; i < items.size(); i++) {
      order.addItemToOrder(items.get(i), itemQuantities.get(i));
    }
    order.finalizeOrder();
    order.setOrderStatus(OrderStatus.COMPLETED);
    org.springframework.test.util.ReflectionTestUtils.setField(order, "orderId", orderId);
    return order;
  }

  private User buildUser(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    return user;
  }

  private Item buildItem(Integer itemId, BigDecimal price, Integer stock) {
    Item item = new Item();
    item.setItemId(itemId);
    item.setPrice(price);
    item.setStock(stock);
    item.setUserId(1);
    item.setName("Test Item " + itemId);
    item.setDescription("Default Description");
    item.setDeleted(false);
    return item;
  }

  private String orderRequestJson(List<Integer> ids, List<Integer> quantities) {
    StringBuilder sb = new StringBuilder("{\"items\": [");
    for (int i = 0; i < ids.size(); i++) {
      sb.append("{\"itemId\":").append(ids.get(i))
          .append(",\"quantity\":").append(quantities.get(i)).append("}");
      if (i < ids.size() - 1) {
        sb.append(",");
      }
    }
    sb.append("]}");
    return sb.toString();
  }
}
