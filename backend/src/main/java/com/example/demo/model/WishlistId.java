package com.example.demo.model;

import java.io.Serializable;
import java.util.Objects;

public class WishlistId implements Serializable {

  private Integer userId;
  private Integer itemId;

  public WishlistId() {}

  public WishlistId(Integer userId, Integer itemId) {
    this.userId = userId;
    this.itemId = itemId;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WishlistId that = (WishlistId) o;
    return Objects.equals(userId, that.userId) && Objects.equals(itemId, that.itemId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, itemId);
  }
}
