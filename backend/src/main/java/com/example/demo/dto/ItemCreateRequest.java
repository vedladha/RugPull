package com.example.demo.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) representing a request to create a new item.
 * Encapsulates the necessary data required from the client to initialize an item record.
 */
public class ItemCreateRequest {

  private String name;
  private String description;
  private BigDecimal price;
  private Integer stock;

  /**
   * Retrieves the name of the item.
   *
   * @return the item name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the item.
   *
   * @param name the item name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Retrieves the description of the item.
   *
   * @return the item description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of the item.
   *
   * @param description the item description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Retrieves the price of the item.
   *
   * @return the item price
   */
  public BigDecimal getPrice() {
    return price;
  }

  /**
   * Sets the price of the item.
   *
   * @param price the item price to set
   */
  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  /**
   * Retrieves the initial stock quantity of the item.
   *
   * @return the item stock
   */
  public Integer getStock() {
    return stock;
  }

  /**
   * Sets the initial stock quantity of the item.
   *
   * @param stock the item stock to set
   */
  public void setStock(Integer stock) {
    this.stock = stock;
  }
}
