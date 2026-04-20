package edu.wisc.t32.wrappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents the summary of an order after purchase.
 */
public class OrderSummary {
  private String orderType; // "buy" or "sell"
  private String buyerName;
  private String sellerName;
  private String itemName;
  private Integer quantity;
  private BigDecimal totalPrice;
  private String createdAt;

  /**
   * Constructor for springboot.
   */
  protected OrderSummary() {
  }

  /**
   * Creats a new order summary.
   *
   * @param orderType  the type of order, buy or sell
   * @param buyerName  the buyer's name
   * @param sellerName the seller's name
   * @param itemName   the item being purchased
   * @param quantity   the quantity of item
   * @param price      the price of the item.
   * @param createdAt  the time the order was created at
   */
  public OrderSummary(String orderType, String buyerName, String sellerName, String itemName,
                      Integer quantity,
                      BigDecimal price, String createdAt) {
    this.orderType = orderType;
    this.buyerName = buyerName;
    this.sellerName = sellerName;
    this.itemName = itemName;
    this.quantity = quantity;
    this.totalPrice = price.multiply(BigDecimal.valueOf(quantity));
    this.createdAt = createdAt;
  }

  public String getOrderType() {
    return orderType;
  }

  public String getBuyerName() {
    return buyerName;
  }

  public String getSellerName() {
    return sellerName;
  }

  public String getItemName() {
    return itemName;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public BigDecimal getTotalPrice() {
    return totalPrice;
  }

  public String getCreatedAt() {
    return createdAt;
  }
}
