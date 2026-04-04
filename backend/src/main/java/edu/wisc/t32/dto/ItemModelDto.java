package edu.wisc.t32.dto;

import edu.wisc.t32.model.Item;
import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for the {@link Item} entity.
 * Used to safely transfer item data to the client layer without exposing
 * internal database representations or Hibernate proxies.
 */
public class ItemModelDto {

  /**
   * The unique identifier for the item.
   */
  private Integer itemId;

  /**
   * The identifier of the user associated with this item.
   */
  private Integer userId;

  /**
   * The price of the item.
   */
  private BigDecimal price;

  /**
   * The name of the item.
   */
  private String name;

  /**
   * A description of the item.
   */
  private String description;

  /**
   * The current stock quantity available for this item.
   */
  private Integer stock;

  /**
   * Flag indicating whether the item has been soft-deleted.
   */
  private Boolean deleted;

  /**
   * Converts an {@link Item} entity into an {@link ItemModelDto}.
   *
   * @param item the Item entity to convert
   * @return a populated ItemModelDTO, or null if the provided item is null
   */
  public static ItemModelDto fromItem(Item item) {
    if (item == null) {
      return null;
    }

    ItemModelDto dto = new ItemModelDto();
    dto.setItemId(item.getItemId());
    dto.setUserId(item.getUserId());
    dto.setPrice(item.getPrice());
    dto.setName(item.getName());
    dto.setDescription(item.getDescription());
    dto.setStock(item.getStock());
    dto.setDeleted(item.getDeleted());

    return dto;
  }

  /**
   * Retrieves the item's unique identifier.
   *
   * @return the item ID
   */
  public Integer getItemId() {
    return itemId;
  }

  /**
   * Sets the item's unique identifier.
   *
   * @param itemId the item ID to set
   */
  public void setItemId(Integer itemId) {
    this.itemId = itemId;
  }

  /**
   * Retrieves the user ID associated with the item.
   *
   * @return the user ID
   */
  public Integer getUserId() {
    return userId;
  }

  /**
   * Sets the user ID associated with the item.
   *
   * @param userId the user ID to set
   */
  public void setUserId(Integer userId) {
    this.userId = userId;
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
   * Retrieves the current stock quantity.
   *
   * @return the stock quantity
   */
  public Integer getStock() {
    return stock;
  }

  /**
   * Sets the current stock quantity.
   *
   * @param stock the stock quantity to set
   */
  public void setStock(Integer stock) {
    this.stock = stock;
  }

  /**
   * Checks if the item is marked as soft-deleted.
   *
   * @return true if deleted, false otherwise
   */
  public Boolean getDeleted() {
    return deleted;
  }

  /**
   * Sets the soft-deletion status of the item.
   *
   * @param deleted the boolean flag to mark deletion status
   */
  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }
}
