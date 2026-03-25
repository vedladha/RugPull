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
 * Entity representing an item in the database.
 * Mapped to the "items" table and includes fields for pricing, inventory,
 * auditing timestamps, and soft deletion tracking.
 */
@Entity
@Table(name = "items")
public class Item {

  /**
   * The unique identifier for the item.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_id")
  private Integer itemId;

  /**
   * The identifier of the user associated with this item.
   */
  @Column(name = "user_id", nullable = false)
  private Integer userId;

  /**
   * The price of the item, configured for high precision and scale.
   */
  @Column(name = "price", precision = 30, scale = 8)
  private BigDecimal price;

  /**
   * The name of the item.
   */
  @Column(name = "name", nullable = false)
  private String name;

  /**
   * A description of the item.
   */
  @Column(name = "description", nullable = false)
  private String description;

  /**
   * The current stock quantity available for this item.
   * Defaults to 0.
   */
  @Column(name = "stock")
  private Integer stock = 0;

  /**
   * The timestamp when this record was created.
   * Managed entirely by the database schema (non-insertable, non-updatable).
   */
  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * The timestamp when this record was last updated.
   * Managed entirely by the database schema (non-insertable, non-updatable).
   */
  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  /**
   * Flag indicating whether the item has been soft-deleted.
   * Defaults to false.
   */
  @Column(name = "deleted", nullable = false)
  private Boolean deleted = false;

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
   * Retrieves the creation timestamp.
   * Note: This value is populated by the database upon insertion.
   *
   * @return the creation timestamp
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * Retrieves the last updated timestamp.
   * Note: This value is populated by the database upon update.
   *
   * @return the update timestamp
   */
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
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
