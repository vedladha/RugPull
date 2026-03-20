package com.example.demo.dto;

/**
 * Data Transfer Object representing a request to create a new order.
 * This request supports a single item per order row.
 */
public class OrderCreateRequest {

  private Integer itemId;
  private Integer quantity;

  /**
   * Retrieves the item ID being ordered.
   *
   * @return the item ID
   */
  public Integer getItemId() {
    return itemId;
  }

  /**
   * Sets the item ID being ordered.
   *
   * @param itemId the item ID to set
   */
  public void setItemId(Integer itemId) {
    this.itemId = itemId;
  }

  /**
   * Retrieves the quantity being ordered.
   *
   * @return the quantity
   */
  public Integer getQuantity() {
    return quantity;
  }

  /**
   * Sets the quantity being ordered.
   *
   * @param quantity the quantity to set
   */
  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }
}
