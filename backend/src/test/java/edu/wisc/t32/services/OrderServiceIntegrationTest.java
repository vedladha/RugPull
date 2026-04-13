package edu.wisc.t32.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wisc.t32.dto.OrderCreateRequest;
import edu.wisc.t32.enums.UserStatus;
import edu.wisc.t32.exception.InsufficientStockException;
import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.Order;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.OrderRepository;
import edu.wisc.t32.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Integration tests for concurrent order placement against the same item stock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

  @Autowired
  private OrderService orderService;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private UserRepository userRepository;

  @TestConfiguration
  static class TestCorsConfiguration {
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
      CorsConfiguration configuration = new CorsConfiguration();
      configuration.addAllowedOrigin("http://localhost:3000");
      configuration.addAllowedHeader("*");
      configuration.addAllowedMethod("*");
      configuration.setAllowCredentials(true);

      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", configuration);
      return source;
    }
  }

  @BeforeEach
  void clearDatabase() {
    orderRepository.deleteAll();
    itemRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createOrder_allowsOnlyOneConcurrentPurchase_whenStockIsOne() throws Exception {
    User seller = userRepository.save(buildUser("seller@example.com"));
    User buyerOne = userRepository.save(buildUser("buyer-one@example.com"));
    User buyerTwo = userRepository.save(buildUser("buyer-two@example.com"));
    
    Item item = itemRepository.save(buildItem(seller.getUserId(), new BigDecimal("12.50"), 1));

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    try {
      Future<PurchaseAttemptResult> firstAttempt =
          executorService.submit(purchaseTask(buyerOne, item.getItemId(), ready, start));
      Future<PurchaseAttemptResult> secondAttempt =
          executorService.submit(purchaseTask(buyerTwo, item.getItemId(), ready, start));

      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();

      PurchaseAttemptResult firstResult = firstAttempt.get(10, TimeUnit.SECONDS);
      PurchaseAttemptResult secondResult = secondAttempt.get(10, TimeUnit.SECONDS);

      List<PurchaseAttemptResult> results = List.of(firstResult, secondResult);
      long successfulPurchases = results.stream().filter(PurchaseAttemptResult::success).count();
      long failedPurchases = results.stream().filter(result -> !result.success()).count();

      assertEquals(1, successfulPurchases, "One purchase should succeed");
      assertEquals(1, failedPurchases, "One purchase should fail due to stock depletion");

      PurchaseAttemptResult failedResult = results.stream()
          .filter(result -> !result.success())
          .findFirst()
          .orElseThrow();
      
      assertTrue(failedResult.errorMessage().equalsIgnoreCase("insufficient stock"));

      List<Order> savedOrders = orderRepository.findAll();
      assertEquals(1, savedOrders.size());
      
      Integer winningBuyerId = savedOrders.get(0).getUser().getUserId();
      assertTrue(winningBuyerId.equals(buyerOne.getUserId()) || winningBuyerId.equals(buyerTwo.getUserId()));

      Item savedItem = itemRepository.findById(item.getItemId()).orElseThrow();
      assertEquals(0, savedItem.getStock(), "Stock should be exactly zero");
      
    } finally {
      executorService.shutdownNow();
    }
  }

  private Callable<PurchaseAttemptResult> purchaseTask(
      User user,
      Integer itemId,
      CountDownLatch ready,
      CountDownLatch start) {
    return () -> {
      ready.countDown();
      if (!start.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting to start concurrent purchase");
      }

      try {
        orderService.createOrder(user, buildRequest(itemId, 1));
        return new PurchaseAttemptResult(true, null);
      } catch (InsufficientStockException exception) {
        return new PurchaseAttemptResult(false, exception.getMessage());
      } catch (Exception e) {
        return new PurchaseAttemptResult(false, e.getMessage());
      }
    };
  }

  private OrderCreateRequest buildRequest(Integer itemId, Integer quantity) {
    OrderCreateRequest.ItemRequest itemReq = new OrderCreateRequest.ItemRequest();
    itemReq.setItemId(itemId);
    itemReq.setQuantity(quantity);

    OrderCreateRequest request = new OrderCreateRequest();
    request.setItems(List.of(itemReq));
    return request;
  }

  private Item buildItem(Integer userId, BigDecimal price, Integer stock) {
    Item item = new Item();
    item.setUserId(userId);
    item.setPrice(price);
    item.setName("Generic Test Item");
    item.setDescription("Description");
    item.setStock(stock);
    item.setDeleted(false);
    return item;
  }

  private User buildUser(String email) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash("hashed-password");
    user.setStatus(UserStatus.ACTIVE);
    user.setDeleted(false);
    return user;
  }

  private record PurchaseAttemptResult(boolean success, String errorMessage) {
  }
}