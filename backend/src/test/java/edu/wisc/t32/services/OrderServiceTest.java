package edu.wisc.t32.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.OrderCreateRequest;
import edu.wisc.t32.exception.InsufficientBalanceException;
import edu.wisc.t32.exception.InsufficientStockException;
import edu.wisc.t32.exception.OrderItemNotFoundException;
import edu.wisc.t32.exception.OrderPaymentException;
import edu.wisc.t32.exception.SelfPurchaseException;
import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.OrderRepository;
import edu.wisc.t32.repository.UserWalletRepository;
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

  @Mock
  private UserWalletRepository userWalletRepository;

  @Mock
  private RpcWalletService walletService;

  @InjectMocks
  private OrderService orderService;

  @Test
  void createOrder_returnsSavedOrder_whenStockAndPaymentSucceed() {
    User buyer = buildUser(7);
    OrderCreateRequest request = buildRequest(4, 2);
    Item item = buildItem(4, new BigDecimal("12.50"), 5, 9);
    UserWallet buyerWallet = buildWallet(7, "buyer-wallet");
    UserWallet sellerWallet = buildWallet(9, "seller-wallet");

    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4)).thenReturn(Optional.of(item));
    when(userWalletRepository.findUserWalletByUserId(7)).thenReturn(Optional.of(buyerWallet));
    when(userWalletRepository.findUserWalletByUserId(9)).thenReturn(Optional.of(sellerWallet));
    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
      Order saved = invocation.getArgument(0);
      org.springframework.test.util.ReflectionTestUtils.setField(saved, "orderId", 1);
      return saved;
    });

    Order saved = orderService.createOrder(buyer, request);

    assertEquals(1, saved.getOrderId());
    assertEquals(7, saved.getUser().getUserId());
    assertEquals(1, saved.getItems().size());
    assertEquals(4, saved.getItems().get(0).getItem().getItemId());
    assertEquals(2, saved.getItems().get(0).getQuantity());
    assertEquals(0, new BigDecimal("25.00").compareTo(saved.getTotalPrice()));
    assertEquals("COMPLETED", saved.getOrderStatus().name());
    assertEquals(3, item.getStock());

    verify(walletService).transferToOperator(buyerWallet, 25.00f);
    verify(walletService).transferFromOperator(sellerWallet, 25.00f);
    verify(orderRepository, times(1)).save(any(Order.class));
  }

  @Test
  void createOrder_usesLockedItemLookup() {
    Item item = buildItem(4, new BigDecimal("12.50"), 5, 9);
    UserWallet buyerWallet = buildWallet(7, "buyer-wallet");
    UserWallet sellerWallet = buildWallet(9, "seller-wallet");

    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4)).thenReturn(Optional.of(item));
    when(userWalletRepository.findUserWalletByUserId(7)).thenReturn(Optional.of(buyerWallet));
    when(userWalletRepository.findUserWalletByUserId(9)).thenReturn(Optional.of(sellerWallet));
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    orderService.createOrder(buildUser(7), buildRequest(4, 1));

    verify(itemRepository, times(1)).findByItemIdAndDeletedFalseForUpdate(4);
  }

  @Test
  void createOrder_throwsNotFound_whenItemDoesNotExist() {
    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(99)).thenReturn(Optional.empty());

    OrderItemNotFoundException error = assertThrows(OrderItemNotFoundException.class,
        () -> orderService.createOrder(buildUser(7), buildRequest(99, 1)));

    assertEquals("Item not found.", error.getMessage());
    verify(orderRepository, never()).save(any(Order.class));
  }

  @Test
  void createOrder_throwsWhenStockIsInsufficient() {
    Item item = buildItem(4, new BigDecimal("12.50"), 5, 9);
    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4)).thenReturn(Optional.of(item));

    InsufficientStockException error = assertThrows(InsufficientStockException.class,
        () -> orderService.createOrder(buildUser(7), buildRequest(4, 6)));

    assertEquals("Insufficient stock", error.getMessage());
    verify(orderRepository, never()).save(any(Order.class));
  }

  @Test
  void createOrder_throwsWhenBuyerAttemptsToPurchaseOwnListing() {
    Item item = buildItem(4, new BigDecimal("12.50"), 5, 7);
    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4)).thenReturn(Optional.of(item));

    SelfPurchaseException error = assertThrows(SelfPurchaseException.class,
        () -> orderService.createOrder(buildUser(7), buildRequest(4, 1)));

    assertEquals("You cannot purchase your own listing.", error.getMessage());
    verify(orderRepository, never()).save(any(Order.class));
    verify(walletService, never()).transferToOperator(any(), anyFloat());
  }

  @Test
  void createOrder_throwsInsufficientBalance_whenBuyerCannotCoverTotal() {
    User buyer = buildUser(7);
    Item item = buildItem(4, new BigDecimal("12.50"), 5, 9);
    UserWallet buyerWallet = buildWallet(7, "buyer-wallet");
    UserWallet sellerWallet = buildWallet(9, "seller-wallet");

    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4)).thenReturn(Optional.of(item));
    when(userWalletRepository.findUserWalletByUserId(7)).thenReturn(Optional.of(buyerWallet));
    when(userWalletRepository.findUserWalletByUserId(9)).thenReturn(Optional.of(sellerWallet));
    doThrow(new IllegalArgumentException("Insufficient balance"))
        .when(walletService).transferToOperator(buyerWallet, 25.00f);

    InsufficientBalanceException error = assertThrows(InsufficientBalanceException.class,
        () -> orderService.createOrder(buyer, buildRequest(4, 2)));

    assertEquals("Insufficient RPC balance.", error.getMessage());
    verify(walletService, never()).transferFromOperator(any(), anyFloat());
    verify(orderRepository, never()).save(any(Order.class));
  }

  @Test
  void createOrder_rollsBackCompletedPayouts_whenLaterSellerPaymentFails() {
    User buyer = buildUser(7);
    OrderCreateRequest request = buildRequest(
        new Integer[] {4, 5},
        new Integer[] {1, 1}
    );
    Item firstItem = buildItem(4, new BigDecimal("10.00"), 5, 9);
    Item secondItem = buildItem(5, new BigDecimal("15.00"), 5, 10);
    UserWallet buyerWallet = buildWallet(7, "buyer-wallet");
    UserWallet firstSellerWallet = buildWallet(9, "seller-wallet-a");
    UserWallet secondSellerWallet = buildWallet(10, "seller-wallet-b");

    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(4))
        .thenReturn(Optional.of(firstItem));
    when(itemRepository.findByItemIdAndDeletedFalseForUpdate(5))
        .thenReturn(Optional.of(secondItem));
    when(userWalletRepository.findUserWalletByUserId(7)).thenReturn(Optional.of(buyerWallet));
    when(userWalletRepository.findUserWalletByUserId(9))
        .thenReturn(Optional.of(firstSellerWallet));
    when(userWalletRepository.findUserWalletByUserId(10))
        .thenReturn(Optional.of(secondSellerWallet));
    doAnswer(invocation -> null)
        .when(walletService).transferFromOperator(
            argThat(wallet -> wallet != null && wallet.getUserId().equals(9)),
            anyFloat());
    doThrow(new IllegalStateException("hedera failed"))
        .when(walletService).transferFromOperator(
            argThat(wallet -> wallet != null && wallet.getUserId().equals(10)),
            anyFloat());

    OrderPaymentException error = assertThrows(OrderPaymentException.class,
        () -> orderService.createOrder(buyer, request));

    assertEquals("Failed to pay one or more sellers.", error.getMessage());
    verify(walletService).transferToOperator(
        argThat(wallet -> wallet != null && wallet.getUserId().equals(7)),
        anyFloat());
    verify(walletService).transferFromOperator(
        argThat(wallet -> wallet != null && wallet.getUserId().equals(9)),
        anyFloat());
    verify(walletService).transferFromOperator(
        argThat(wallet -> wallet != null && wallet.getUserId().equals(10)),
        anyFloat());
    verify(walletService).transferToOperator(
        argThat(wallet -> wallet != null && wallet.getUserId().equals(9)),
        anyFloat());
    verify(walletService).transferFromOperator(
        argThat(wallet -> wallet != null && wallet.getUserId().equals(7)),
        anyFloat());
    verify(orderRepository, never()).save(any(Order.class));
  }

  private OrderCreateRequest buildRequest(Integer itemId, Integer quantity) {
    return buildRequest(new Integer[] {itemId}, new Integer[] {quantity});
  }

  private OrderCreateRequest buildRequest(Integer[] itemIds, Integer[] quantities) {
    java.util.List<OrderCreateRequest.ItemRequest> items = new java.util.ArrayList<>();
    for (int index = 0; index < itemIds.length; index++) {
      OrderCreateRequest.ItemRequest itemReq = new OrderCreateRequest.ItemRequest();
      itemReq.setItemId(itemIds[index]);
      itemReq.setQuantity(quantities[index]);
      items.add(itemReq);
    }

    OrderCreateRequest request = new OrderCreateRequest();
    request.setItems(items);
    return request;
  }

  private Item buildItem(Integer itemId, BigDecimal price, Integer stock, Integer sellerUserId) {
    Item item = new Item();
    item.setItemId(itemId);
    item.setPrice(price);
    item.setStock(stock);
    item.setDeleted(false);
    item.setUserId(sellerUserId);
    return item;
  }

  private User buildUser(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    return user;
  }

  private UserWallet buildWallet(Integer userId, String walletAddress) {
    UserWallet wallet = new UserWallet();
    wallet.setUserId(userId);
    wallet.setWalletAddress(walletAddress);
    wallet.setWalletPrivateKey("private-key-" + userId);
    return wallet;
  }
}
