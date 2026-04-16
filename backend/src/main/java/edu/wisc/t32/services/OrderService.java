package edu.wisc.t32.services;

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
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.OrderRepository;
import edu.wisc.t32.repository.UserWalletRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating orders while protecting stock updates with item-row locks and payment
 * orchestration.
 */
@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final ItemRepository itemRepository;
  private final UserWalletRepository userWalletRepository;
  private final RpcWalletService walletService;

  /**
   * Constructs the service with the repositories and wallet service needed to place orders.
   *
   * @param orderRepository repository used for saving orders
   * @param itemRepository repository used for locked item lookups and stock updates
   * @param userWalletRepository repository used for loading buyer and seller wallets
   * @param walletService service used for executing RPC transfers
   */
  public OrderService(OrderRepository orderRepository,
                      ItemRepository itemRepository,
                      UserWalletRepository userWalletRepository,
                      RpcWalletService walletService) {
    this.orderRepository = orderRepository;
    this.itemRepository = itemRepository;
    this.userWalletRepository = userWalletRepository;
    this.walletService = walletService;
  }

  /**
   * Creates a new order for the supplied authenticated user.
   *
   * <p>The item row is locked for the duration of the transaction so stock can be checked and
   * decremented safely under concurrent purchase attempts. The order is only persisted after the
   * corresponding RPC transfers have completed successfully.
   *
   * @param currentUser the authenticated user placing the order
   * @param request the validated order request payload
   * @return the saved order
   * @throws OrderItemNotFoundException when the item does not exist
   * @throws InsufficientStockException when there is not enough stock left to fulfill the request
   */
  @Transactional
  public Order createOrder(User currentUser, OrderCreateRequest request) {
    validateRequest(request);

    Order order = new Order(currentUser, OrderStatus.PENDING);
    Map<Integer, SellerPayout> sellerPayouts = new LinkedHashMap<>();

    for (OrderCreateRequest.ItemRequest itemRequest : request.getItems()) {
      Item item = itemRepository.findByItemIdAndDeletedFalseForUpdate(itemRequest.getItemId())
          .orElseThrow(() -> new OrderItemNotFoundException("Item not found."));

      if (currentUser.getUserId().equals(item.getUserId())) {
        throw new SelfPurchaseException("You cannot purchase your own listing.");
      }

      reserveStock(item, itemRequest.getQuantity());
      order.addItemToOrder(item, itemRequest.getQuantity());

      BigDecimal lineTotal = item.getPrice()
          .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
      if (lineTotal.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      UserWallet sellerWallet = sellerPayouts.containsKey(item.getUserId())
          ? sellerPayouts.get(item.getUserId()).wallet()
          : requireWallet(item.getUserId(), "Seller wallet is not configured.");

      sellerPayouts.merge(
          item.getUserId(),
          new SellerPayout(sellerWallet, lineTotal),
          (existing, incoming) ->
              new SellerPayout(existing.wallet(), existing.amount().add(incoming.amount())));
    }

    order.finalizeOrder();
    processPayment(currentUser, order.getTotalPrice(), new ArrayList<>(sellerPayouts.values()));
    order.setOrderStatus(OrderStatus.COMPLETED);

    return orderRepository.save(order);
  }

  /**
   * Gets all orders previously placed by a user.
   *
   * @param currentUser the authenticated user to get the order history of
   * @return a list containing all orders placed by the user
   */
  public List<Order> getOrderHistory(User currentUser) {
    return orderRepository.findByUserOrderByCreatedAtDesc(currentUser);
  }

  /**
   * Checks that the quantity is valid for the current stock of an item and reduces the stock by
   * that quantity.
   *
   * @param item The item to check the stock of
   * @param quantity The quantity of the item wanted
   * @throws InsufficientStockException when there is less stock than the quantity requested
   * @throws IllegalArgumentException if the quantity is not positive
   */
  public void reserveStock(Item item, Integer quantity) {
    if (quantity == null || quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be positive.");
    }
    if (item.getStock() < quantity) {
      throw new InsufficientStockException("Insufficient stock");
    }
    item.setStock(item.getStock() - quantity);
  }

  /**
   * Validates the order request to ensure it contains at least one item and that all item details
   * are present and valid.
   *
   * @param request the order payload to validate
   * @throws IllegalArgumentException if the request is null, empty, or contains invalid data
   */
  private void validateRequest(OrderCreateRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Order request cannot be null.");
    }

    if (request.getItems() == null || request.getItems().isEmpty()) {
      throw new IllegalArgumentException("Order must contain at least one item.");
    }

    for (OrderCreateRequest.ItemRequest item : request.getItems()) {
      if (item.getItemId() == null) {
        throw new IllegalArgumentException("itemId is required for all items.");
      }
      if (item.getQuantity() == null) {
        throw new IllegalArgumentException("quantity is required for all items.");
      }
      if (item.getQuantity() <= 0) {
        throw new IllegalArgumentException("quantity must be greater than 0.");
      }
    }
  }

  private void processPayment(User currentUser, BigDecimal totalAmount,
                              List<SellerPayout> sellerPayouts) {
    if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }

    UserWallet buyerWallet = requireWallet(
        currentUser.getUserId(),
        "Buyer wallet is not configured.");
    float buyerCharge = toTransferAmount(totalAmount, "Order total");

    try {
      walletService.transferToOperator(buyerWallet, buyerCharge);
    } catch (IllegalArgumentException exception) {
      throw new InsufficientBalanceException("Insufficient RPC balance.");
    } catch (IllegalStateException exception) {
      throw new OrderPaymentException("Failed to collect buyer payment.", exception);
    }

    List<SellerPayout> completedPayouts = new ArrayList<>();
    for (SellerPayout payout : sellerPayouts) {
      float sellerAmount = toTransferAmount(payout.amount(), "Seller payout");
      try {
        walletService.transferFromOperator(payout.wallet(), sellerAmount);
        completedPayouts.add(payout);
      } catch (RuntimeException exception) {
        rollbackPayment(buyerWallet, totalAmount, completedPayouts);
        throw new OrderPaymentException("Failed to pay one or more sellers.", exception);
      }
    }
  }

  private void rollbackPayment(UserWallet buyerWallet, BigDecimal totalAmount,
                               List<SellerPayout> completedPayouts) {
    RuntimeException rollbackFailure = null;

    for (int index = completedPayouts.size() - 1; index >= 0; index--) {
      SellerPayout payout = completedPayouts.get(index);
      try {
        walletService.transferToOperator(
            payout.wallet(),
            toTransferAmount(payout.amount(), "Seller rollback"));
      } catch (RuntimeException exception) {
        if (rollbackFailure == null) {
          rollbackFailure = new OrderPaymentException(
              "Automatic checkout rollback failed while reversing seller payouts.", exception);
        } else {
          rollbackFailure.addSuppressed(exception);
        }
      }
    }

    try {
      walletService.transferFromOperator(
          buyerWallet,
          toTransferAmount(totalAmount, "Buyer refund"));
    } catch (RuntimeException exception) {
      if (rollbackFailure == null) {
        rollbackFailure = new OrderPaymentException(
            "Automatic checkout rollback failed while refunding the buyer.", exception);
      } else {
        rollbackFailure.addSuppressed(exception);
      }
    }

    if (rollbackFailure != null) {
      throw new OrderPaymentException(
          "Checkout payment failed and rollback was not fully completed.", rollbackFailure);
    }
  }

  private UserWallet requireWallet(Integer userId, String message) {
    return userWalletRepository.findUserWalletByUserId(userId)
        .orElseThrow(() -> new OrderPaymentException(message));
  }

  private float toTransferAmount(BigDecimal amount, String label) {
    try {
      return amount.setScale(2, RoundingMode.UNNECESSARY).floatValue();
    } catch (ArithmeticException exception) {
      throw new OrderPaymentException(label + " exceeds RPC precision.", exception);
    }
  }

  private record SellerPayout(UserWallet wallet, BigDecimal amount) {
  }
}
