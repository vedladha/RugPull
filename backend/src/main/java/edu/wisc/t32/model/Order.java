package edu.wisc.t32.model;

import edu.wisc.t32.enums.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an order in the database.
 *
 * <p>Mapped to the {@code orders} table and acts as a parent for multiple order items.
 */
@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_id")
  private Integer orderId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", updatable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_status", nullable = false)
  private OrderStatus orderStatus = OrderStatus.PENDING;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  @Column(name = "total_price", nullable = false, precision = 30, scale = 8)
  private BigDecimal totalPrice = BigDecimal.ZERO;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  /**
   * Default constructor for JPA.
   */
  protected Order() {
  }

  /**
   * Creates an order for the given user.
   *
   * @param user the user who owns the order
   */
  public Order(User user) {
    this.user = user;
  }

  /**
   * Creates an order for the given user with an explicit starting status.
   *
   * @param user the user who owns the order
   * @param orderStatus the initial order status
   */
  public Order(User user, OrderStatus orderStatus) {
    this.user = user;
    this.orderStatus = orderStatus;
  }

  public Integer getOrderId() {
    return orderId;
  }

  public User getUser() {
    return user;
  }

  public OrderStatus getOrderStatus() {
    return orderStatus;
  }

  public List<OrderItem> getItems() {
    return items;
  }

  public BigDecimal getTotalPrice() {
    return totalPrice;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setOrderStatus(OrderStatus orderStatus) {
    this.orderStatus = orderStatus;
  }

  /**
   * Finalizes the order by calculating its total price and setting the next status.
   */
  public void finalizeOrder() {
    BigDecimal calculatedTotalPrice = BigDecimal.ZERO;
    for (OrderItem item : items) {
      calculatedTotalPrice = calculatedTotalPrice.add(
          item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
      );
    }
    this.totalPrice = calculatedTotalPrice;
    this.orderStatus = OrderStatus.AWAITING_CONFIRMATION;
  }

  /**
   * Adds an item to this order and updates the running total.
   *
   * @param item the item being added
   * @param quantity the quantity of the item
   */
  public void addItemToOrder(Item item, Integer quantity) {
    OrderItem newItem = new OrderItem(this, item, quantity, item.getPrice());
    this.items.add(newItem);
    this.totalPrice = this.totalPrice.add(
        newItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity))
    );
  }
}
