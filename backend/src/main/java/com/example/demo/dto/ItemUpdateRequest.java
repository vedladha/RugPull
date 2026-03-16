package com.example.demo.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) representing a request to update an existing item.
 * Encapsulates the data fields that can be modified by the client.
 */
public class ItemUpdateRequest {

  private String name;
  private String description;
  private BigDecimal price;
  private Integer stock;

  /**
   * Retrieves the updated name of the item.
   *
   * @return the new item name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the updated name of the item.
   *
   * @param name the new item name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Retrieves the updated description of the item.
   *
   * @return the new item description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the updated description of the item.
   *
   * @param description the new item description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Retrieves the updated price of the item.
   *
   * @return the new item price
   */
  public BigDecimal getPrice() {
    return price;
  }

  /**
   * Sets the updated price of the item.
   *
   * @param price the new item price to set
   */
  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  /**
   * Retrieves the updated stock quantity of the item.
   *
   * @return the new item stock
   */
  public Integer getStock() {
    return stock;
  }

  /**
   * Sets the updated stock quantity of the item.
   *
   * @param stock the new item stock to set
   */
  public void setStock(Integer stock) {
    this.stock = stock;
  }
}
