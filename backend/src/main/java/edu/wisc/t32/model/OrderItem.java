package edu.wisc.t32.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Entity representing an order item in the database.
 *
 * <p>Mapped to the {@code order_items} table and stores a single item as part of a larger order.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_item_id")
  private Integer orderItemId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  @JsonIgnore
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "item_id")
  private Item item;

  @Column(name = "quantity")
  private Integer quantity = 1;

  @Column(name = "unit_price", nullable = false, precision = 30, scale = 8)
  private BigDecimal unitPrice;

  /**
   * Default constructor for JPA.
   */
  protected OrderItem() {
  }

  /**
   * Creates a new order line item.
   *
   * @param order the parent order
   * @param item the item being ordered
   * @param quantity the quantity ordered
   * @param unitPrice the unit price captured at checkout
   */
  public OrderItem(Order order, Item item, Integer quantity, BigDecimal unitPrice) {
    this.order = order;
    this.item = item;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
  }

  public Integer getOrderItemId() {
    return orderItemId;
  }

  public Order getOrder() {
    return order;
  }

  public Item getItem() {
    return item;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  /**
   * Internal setter used by the order parent class.
   *
   * @param order the parent order
   */
  protected void setOrder(Order order) {
    this.order = order;
  }
}
