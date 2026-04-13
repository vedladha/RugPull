package edu.wisc.t32.dto;

import edu.wisc.t32.model.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data Transfer object for an item batch request.
 */
public class ItemBatchRequest {
  private List<ItemModelDto> items = new ArrayList<>();

  public List<ItemModelDto> getItems() {
    return items;
  }

  /**
   * Completely overrides the items set for this batch request dto.
   *
   * @param items an items list, must not be null.
   */
  public void setItems(List<ItemModelDto> items) {
    Objects.requireNonNull(items, "Can not set ItemBatchRequest items to null");
    this.items = items;
  }

  /**
   * Gets a new empty batch request.
   *
   * @return the brand new batch request
   */
  public static ItemBatchRequest next() {
    return new ItemBatchRequest();
  }
}
