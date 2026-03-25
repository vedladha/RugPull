package edu.wisc.t32.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an order in the database.
 * Mapped to the "Orders" table and stores a single item per order row.
 */
@Entity
@Table(name = "Orders")
public class Order {

  /**
   * The unique identifier for the order.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_id")
  private Integer orderId;

  /**
   * The identifier of the user who placed the order.
   */
  @Column(name = "user_id")
  private Integer userId;

  /**
   * The identifier of the item included in the order.
   */
  @Column(name = "item_id")
  private Integer itemId;

  /**
   * The quantity of the item ordered.
   */
  @Column(name = "quantity")
  private Integer quantity = 1;

  /**
   * The item price captured at the time the order was placed.
   */
  @Column(name = "price", nullable = false, precision = 30, scale = 8)
  private BigDecimal price;

  /**
   * The marketplace fee percentage applied to the order.
   */
  @Column(name = "fee_percentage", precision = 5, scale = 2)
  private BigDecimal feePercentage = new BigDecimal("2.50");

  /**
   * The current status of the order.
   */
  @Column(name = "order_status", nullable = false)
  private String orderStatus = "pending";

  /**
   * The timestamp when the order record was created.
   * Managed entirely by the database schema.
   */
  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * The timestamp when the order record was last updated.
   * Managed entirely by the database schema.
   */
  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  /**
   * Retrieves the unique identifier for the order.
   *
   * @return the order ID
   */
  public Integer getOrderId() {
    return orderId;
  }

  /**
   * Sets the unique identifier for the order.
   *
   * @param orderId the order ID to set
   */
  public void setOrderId(Integer orderId) {
    this.orderId = orderId;
  }

  /**
   * Retrieves the user ID associated with the order.
   *
   * @return the user ID
   */
  public Integer getUserId() {
    return userId;
  }

  /**
   * Sets the user ID associated with the order.
   *
   * @param userId the user ID to set
   */
  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  /**
   * Retrieves the item ID associated with the order.
   *
   * @return the item ID
   */
  public Integer getItemId() {
    return itemId;
  }

  /**
   * Sets the item ID associated with the order.
   *
   * @param itemId the item ID to set
   */
  public void setItemId(Integer itemId) {
    this.itemId = itemId;
  }

  /**
   * Retrieves the order quantity.
   *
   * @return the quantity
   */
  public Integer getQuantity() {
    return quantity;
  }

  /**
   * Sets the order quantity.
   *
   * @param quantity the quantity to set
   */
  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  /**
   * Retrieves the item price captured on the order.
   *
   * @return the order price
   */
  public BigDecimal getPrice() {
    return price;
  }

  /**
   * Sets the item price captured on the order.
   *
   * @param price the price to set
   */
  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  /**
   * Retrieves the marketplace fee percentage applied to the order.
   *
   * @return the fee percentage
   */
  public BigDecimal getFeePercentage() {
    return feePercentage;
  }

  /**
   * Sets the marketplace fee percentage applied to the order.
   *
   * @param feePercentage the fee percentage to set
   */
  public void setFeePercentage(BigDecimal feePercentage) {
    this.feePercentage = feePercentage;
  }

  /**
   * Retrieves the current order status.
   *
   * @return the order status
   */
  public String getOrderStatus() {
    return orderStatus;
  }

  /**
   * Sets the current order status.
   *
   * @param orderStatus the order status to set
   */
  public void setOrderStatus(String orderStatus) {
    this.orderStatus = orderStatus;
  }

  /**
   * Retrieves the timestamp when the order was created.
   *
   * @return the creation timestamp
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * Retrieves the timestamp when the order was last updated.
   *
   * @return the last update timestamp
   */
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
