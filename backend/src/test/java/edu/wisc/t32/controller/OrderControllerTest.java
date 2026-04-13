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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.wisc.t32.dto.OrderCreateRequest;
import edu.wisc.t32.exception.InsufficientStockException;
import edu.wisc.t32.exception.OrderItemNotFoundException;
import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.OrderRepository;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.OrderService;
import java.math.BigDecimal;
import java.util.ArrayList;
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
    // Setup Data
    User currentUser = buildUser(7);
    Item laptop = buildItem(4, new BigDecimal("12.50"), 10);
    Order savedOrder = buildOrder(1, currentUser, List.of(laptop), List.of(2));

    // Mocking
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(any(User.class), any(OrderCreateRequest.class))).thenReturn(savedOrder);

    // Execution & Assertion
    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(4), List.of(2))))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.order.orderId").value(1))
        .andExpect(jsonPath("$.order.totalPrice").value(25.00))
        .andExpect(jsonPath("$.order.items[0].item.itemId").value(4))
        .andExpect(jsonPath("$.order.orderStatus").value("AWAITING_CONFIRMATION"));
  }

  // Checks that creating an order without auth returns 401.
  @Test
  void createOrder_returnsUnauthorized_whenUserIsNotAuthenticated() throws Exception {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    mockMvc.perform(post("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(4), List.of(2))))
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
            .content("{\"items\": [{\"quantity\": 2}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("An itemId is required for all items."));
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
            .content("{\"items\": [{\"itemId\": 4}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Quantity is required for all items."));
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
            .content(orderRequestJson(List.of(4), List.of(0))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Quantity must be greater than 0 for all items."));
    verify(orderService, never()).createOrder(any(User.class), any(OrderCreateRequest.class));
  }

  // Checks that ordering a missing item returns 404.
  @Test
  void createOrder_returnsNotFound_whenItemDoesNotExist() throws Exception {
    User currentUser = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(currentUser));
    when(orderService.createOrder(any(User.class), argThat(request ->
        request.getItems().get(0).getItemId().equals(99) && request.getItems().get(0).getQuantity().equals(2))))
        .thenThrow(new OrderItemNotFoundException("Item not found"));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(99), List.of(2))))
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
        request.getItems().get(0).getItemId().equals(4) && request.getItems().get(0).getQuantity().equals(6))))
        .thenThrow(new InsufficientStockException("insufficient stock"));

    mockMvc.perform(post("/orders")
            .cookie(new jakarta.servlet.http.Cookie("jwt", VALID_TOKEN))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderRequestJson(List.of(4), List.of(6))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("insufficient stock"));
  }

  // Checks that listing orders returns the authenticated user's orders.
  @Test
  void getOrders_returnsOrdersForAuthenticatedUser() {
    // Setup Data
    User user = buildUser(7);
    
    // Items for Order 1
    Item itemA = buildItem(101, new BigDecimal("10.00"), 100);
    Item itemB = buildItem(102, new BigDecimal("20.00"), 100);
    
    // Item for Order 2
    Item itemC = buildItem(103, new BigDecimal("30.00"), 100);

    // Assemble the orders
    Order order1 = buildOrder(1, user, List.of(itemA, itemB), List.of(1, 1));
    Order order2 = buildOrder(2, user, List.of(itemC), List.of(2));

    // Mocking
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(orderService.getOrderHistory(user)).thenReturn(List.of(order1, order2));

    // Execution
    ResponseEntity<?> response = orderController.getOrders(VALID_TOKEN);

    // Assertions
    assertEquals(HttpStatus.OK, response.getStatusCode());
    
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    
    List<?> orders = (List<?>) body.get("orders");
    assertNotNull(orders);
    assertEquals(2, orders.size());
    
    // Verify first order
    Order firstReturned = (Order) orders.get(0);
    assertEquals(1, firstReturned.getOrderId());
    assertEquals(2, firstReturned.getItems().size());
    assertEquals(new BigDecimal("30.00"), firstReturned.getTotalPrice());
  }

  // Checks that listing orders returns an empty list when the user has none.
  @Test
  void getOrders_returnsEmptyList_whenUserHasNoOrders() {
    // Setup Data
    User user = buildUser(7);

    // Mocking
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));

    // Execution
    ResponseEntity<?> response = orderController.getOrders(VALID_TOKEN);

    // Assertions
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
    // Setup Data
    User user = buildUser(7);
    Item itemA = buildItem(101, new BigDecimal("10.00"), 100);
    Order order = buildOrder(5, user, List.of(itemA), List.of(2));

    // Mocking
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(orderRepository.findByOrderIdAndUser(5, user))
        .thenReturn(Optional.of(order));

    // Execution
    ResponseEntity<?> response = orderController.getOrder(VALID_TOKEN, 5);

    // Assertions
    assertEquals(HttpStatus.OK, response.getStatusCode());
    
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    
    Order returnedOrder = (Order) body.get("order");
    assertNotNull(returnedOrder);
    assertEquals(5, returnedOrder.getOrderId());
    assertEquals(7, returnedOrder.getUser().getUserId()); 
    assertEquals(new BigDecimal("20.00"), returnedOrder.getTotalPrice());
  }

  // Checks that getting a missing order returns 404.
  @Test
  void getOrder_returnsNotFound_whenOrderDoesNotExist() {
    // Setup Data
    User user = buildUser(7);

    // Mocking
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(orderRepository.findByOrderIdAndUser(9, user))
        .thenReturn(Optional.empty());

    // Execution
    ResponseEntity<?> response = orderController.getOrder(VALID_TOKEN, 9);

    // Assertions
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Order not found", body.get("error"));
  }

  // Checks that getting another user's order returns 404.
  @Test
  void getOrder_returnsNotFound_whenOrderBelongsToAnotherUser() {
    // Setup Data
    User user = buildUser(7);

    // Mocking
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(orderRepository.findByOrderIdAndUser(11, user))
        .thenReturn(Optional.empty());

    // Execution
    ResponseEntity<?> response = orderController.getOrder(VALID_TOKEN, 11);

    // Assertions
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Order not found", body.get("error"));
  }

  // Checks that getting one order without auth returns 401.
  @Test
  void getOrder_returnsUnauthorized_whenUserIsNotAuthenticated() {
    // Mocking
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    // Execution
    ResponseEntity<?> response = orderController.getOrder(null, 5);

    // Assertions
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
    
    // Verify the database wasn't touched
    verify(orderRepository, never()).findByOrderIdAndUser(anyInt(), any(User.class));
  }

  private OrderCreateRequest buildMultiItemRequest(Map<Integer, Integer> idToQuantity) {
    List<OrderCreateRequest.ItemRequest> itemRequests = new ArrayList<>();

    idToQuantity.forEach((id, qty) -> {
        OrderCreateRequest.ItemRequest itemReq = new OrderCreateRequest.ItemRequest();
        itemReq.setItemId(id);
        itemReq.setQuantity(qty);
        itemRequests.add(itemReq);
    });

    OrderCreateRequest request = new OrderCreateRequest();
    request.setItems(itemRequests);
    return request;
  }

  private Order buildOrder(Integer orderId, User user, List<Item> items, List<Integer> itemQuantities) {
    Order order = new Order(user);
    for (int i = 0; i < items.size(); i++) {
      order.addItemToOrder(items.get(i), itemQuantities.get(i));
    }
    order.finalizeOrder();
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
      if (i < ids.size() - 1) sb.append(",");
    }
    sb.append("]}");
    return sb.toString();
  }
}
