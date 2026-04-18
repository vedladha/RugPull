package edu.wisc.t32.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entity representing an image entry in the database.
 * Mapped to the "item_images" table.
 */
@Entity
@Table(name = "item_images")
public class ItemImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "image_id")
  private Integer imageId;

  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(name = "item_id", nullable = false)
  private Integer itemId;

  @Column(name = "image_url", nullable = false)
  private String imageUrl;

  @Column(name = "alt_text", nullable = false)
  private String altText;

  @Column(name = "position")
  private Integer position = 0;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false)
  private LocalDateTime updatedAt;

  public Integer getImageId() {
    return imageId;
  }

  public void setImageId(Integer imageId) {
    this.imageId = imageId;
  }

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

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getAltText() {
	return altText;
  }

  public void setAltText(String altText) {
	this.altText = altText;
  }

  public Integer getPosition() {
	return position;
  }

  public void setPosition(Integer position) {
	this.position = position;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
	return updatedAt;
  }
}
