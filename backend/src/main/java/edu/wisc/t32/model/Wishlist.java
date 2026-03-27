package edu.wisc.t32.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entity representing a wishlist entry in the database.
 * Mapped to the "Wishlists" table with a composite primary key of (user_id, item_id).
 */
@Entity
@Table(name = "Wishlists")
@IdClass(WishlistId.class)
public class Wishlist {

  @Id
  @Column(name = "user_id")
  private Integer userId;

  @Id
  @Column(name = "item_id")
  private Integer itemId;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public Integer getItemId() {
    return itemId;
  }

  public void setItemId(Integer itemId) {
    this.itemId = itemId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
