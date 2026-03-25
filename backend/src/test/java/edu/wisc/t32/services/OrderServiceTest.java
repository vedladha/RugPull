package edu.wisc.t32.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.OrderCreateRequest;
import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
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
    User user = buildUser(7);
    OrderCreateRequest request = buildRequest(4, 2);
    Item item = buildItem(4, new BigDecimal("12.50"), 5);

    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4)).thenReturn(Optional.of(item));
    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
      Order saved = invocation.getArgument(0);
      saved.setOrderId(1);
      return saved;
    });

    Order saved = orderService.createOrder(user, request);

    assertEquals(1, saved.getOrderId());
    assertEquals(7, saved.getUserId());
    assertEquals(4, saved.getItemId());
    assertEquals(2, saved.getQuantity());
    assertEquals(0, new BigDecimal("12.50").compareTo(saved.getPrice()));
    assertEquals(0, new BigDecimal("2.50").compareTo(saved.getFeePercentage()));
    assertEquals("pending", saved.getOrderStatus());
    assertEquals(3, item.getStock());
    verify(itemRepository, times(1)).save(item);
    verify(orderRepository, times(1)).save(any(Order.class));
  }

  @Test
  void createOrder_usesLockedItemLookup() {
    final User user = buildUser(7);
    final OrderCreateRequest request = buildRequest(4, 1);
    Item item = buildItem(4, new BigDecimal("12.50"), 5);

    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4)).thenReturn(Optional.of(item));
    when(itemRepository.save(any(Item.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    orderService.createOrder(user, request);

    verify(itemRepository, times(1)).findByItemIdAndDeletedFalseForUpdate(4);
    verify(itemRepository, never()).findByItemIdAndDeletedFalse(4);
  }

  @Test
  void createOrder_throwsNotFound_whenItemDoesNotExist() {
    User user = buildUser(7);
    OrderCreateRequest request = buildRequest(99, 1);

    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(99)).thenReturn(Optional.empty());

    NoSuchElementException error = assertThrows(NoSuchElementException.class,
        () -> orderService.createOrder(user, request));

    assertEquals("Item not found", error.getMessage());
    verify(itemRepository, never()).save(any(Item.class));
    verify(orderRepository, never()).save(any(Order.class));
  }

  @Test
  void createOrder_throwsWhenStockIsInsufficient() {
    User user = buildUser(7);
    OrderCreateRequest request = buildRequest(4, 6);
    Item item = buildItem(4, new BigDecimal("12.50"), 5);

    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4)).thenReturn(Optional.of(item));

    IllegalStateException error = assertThrows(IllegalStateException.class,
        () -> orderService.createOrder(user, request));

    assertEquals("insufficient stock", error.getMessage());
    verify(itemRepository, never()).save(any(Item.class));
    verify(orderRepository, never()).save(any(Order.class));
  }

  private OrderCreateRequest buildRequest(Integer itemId, Integer quantity) {
    OrderCreateRequest request = new OrderCreateRequest();
    request.setItemId(itemId);
    request.setQuantity(quantity);
    return request;
  }

  private Item buildItem(Integer itemId, BigDecimal price, Integer stock) {
    Item item = new Item();
    item.setItemId(itemId);
    item.setPrice(price);
    item.setStock(stock);
    item.setDeleted(false);
    return item;
  }

  private User buildUser(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    return user;
  }
}
