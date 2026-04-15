package edu.wisc.t32.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.OrderCreateRequest;
import edu.wisc.t32.exception.InsufficientStockException;
import edu.wisc.t32.exception.OrderItemNotFoundException;
import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private ItemRepository itemRepository;

  @InjectMocks
  private OrderService orderService;

  @Test
  void createOrder_returnsSavedOrder_whenStockIsAvailable() {
    // Setup Data
    User user = buildUser(7);
    OrderCreateRequest request = buildRequest(4, 2);
    Item item = buildItem(4, new BigDecimal("12.50"), 5);

    // Mocking
    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4)).thenReturn(Optional.of(item));
    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
      Order saved = invocation.getArgument(0);
      org.springframework.test.util.ReflectionTestUtils.setField(saved, "orderId", 1);
      return saved;
    });

    // Execution
    Order saved = orderService.createOrder(user, request);

    // Assertions
    assertEquals(1, saved.getOrderId());
    assertEquals(7, saved.getUser().getUserId());

    // Check the OrderItem list
    assertEquals(1, saved.getItems().size());
    assertEquals(4, saved.getItems().get(0).getItem().getItemId());
    assertEquals(2, saved.getItems().get(0).getQuantity());

    // Check calculated fields
    assertEquals(0, new BigDecimal("25.00").compareTo(saved.getTotalPrice()));
    assertEquals("AWAITING_CONFIRMATION", saved.getOrderStatus().name());

    // Check side effects
    assertEquals(3, item.getStock());
    verify(orderRepository, times(1)).save(any(Order.class));
  }

  @Test
  void createOrder_usesLockedItemLookup() {
    User user = buildUser(7);
    OrderCreateRequest request = buildRequest(4, 1);
    Item item = buildItem(4, new BigDecimal("12.50"), 5);

    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4)).thenReturn(Optional.of(item));
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    orderService.createOrder(user, request);

    verify(itemRepository, times(1)).findByItemIdAndDeletedFalseForUpdate(4);
  }

  @Test
  void createOrder_throwsNotFound_whenItemDoesNotExist() {
    User user = buildUser(7);
    OrderCreateRequest request = buildRequest(99, 1);

    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(99)).thenReturn(Optional.empty());

    OrderItemNotFoundException error = assertThrows(OrderItemNotFoundException.class,
        () -> orderService.createOrder(user, request));

    assertEquals("Item not found.", error.getMessage());
    verify(orderRepository, never()).save(any(Order.class));
  }

  @Test
  void createOrder_throwsWhenStockIsInsufficient() {
    User user = buildUser(7);
    OrderCreateRequest request = buildRequest(4, 6); // Asking for 6, only 5 in stock
    Item item = buildItem(4, new BigDecimal("12.50"), 5);

    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4)).thenReturn(Optional.of(item));

    InsufficientStockException error = assertThrows(InsufficientStockException.class,
        () -> orderService.createOrder(user, request));

    assertEquals("Insufficient stock", error.getMessage());
    verify(orderRepository, never()).save(any(Order.class));
  }

  private OrderCreateRequest buildRequest(Integer itemId, Integer quantity) {
    OrderCreateRequest.ItemRequest itemReq = new OrderCreateRequest.ItemRequest();
    itemReq.setItemId(itemId);
    itemReq.setQuantity(quantity);

    OrderCreateRequest request = new OrderCreateRequest();
    request.setItems(java.util.List.of(itemReq));
    return request;
  }

  private Item buildItem(Integer itemId, BigDecimal price, Integer stock) {
    Item item = new Item();
    item.setItemId(itemId);
    item.setPrice(price);
    item.setStock(stock);
    item.setDeleted(false);
    item.setUserId(1);
    return item;
  }

  private User buildUser(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    return user;
  }
}
