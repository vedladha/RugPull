package edu.wisc.t32.dto;

import java.util.List;

/**
 * Request payload for creating a new order.
 * Contains a list of items within the order.
 */
public class OrderCreateRequest {
  private List<ItemRequest> items;

  public List<ItemRequest> getItems() {
    return items;
  }

  public void setItems(List<ItemRequest> items) {
    this.items = items;
  }

  /**
   * Individual line item in the order request.
   */
  public static class ItemRequest {
    private Integer itemId;
    private Integer quantity;

    public Integer getItemId() {
      return itemId;
    }

    public void setItemId(Integer itemId) {
      this.itemId = itemId;
    }

    public Integer getQuantity() {
      return quantity;
    }

    public void setQuantity(Integer quantity) {
      this.quantity = quantity;
    }
  }
}
